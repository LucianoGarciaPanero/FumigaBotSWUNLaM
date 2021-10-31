const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();


exports.programadaNueva = functions.database
    .ref("fumigaciones_programadas/{robotId}/{fumigacionId}")
    .onCreate((snapshot, context) => {
      const robotId = context.params.robotId;
      const fumigacionId = context.params.fumigacionId;
      const timestamp = snapshot.val().timestampInicio;
      // verificarFumigacion retorna promesa:
      return verificarFumigacion(robotId, fumigacionId, timestamp)
          .then(() => {
            // si la promesa sale bien, hacemos:
            console.log("Se agregó la fumigación " + snapshot.key);
            return Promise.resolve();
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, borramos snapshot ref: " + snapshot.ref);
            return snapshot.ref.remove();
          });
    });

exports.programadaUpdate = functions.database
    .ref("fumigaciones_programadas/{robotId}/{fumigacionId}")
    .onUpdate((cambios, context) => {
      const robotId = context.params.robotId;
      const fumigacionId = context.params.fumigacionId;
      // tomamos los valores antes y después
      const antes = cambios.before.val();
      const despues = cambios.after.val();
      // hacemos la comparación para que no haga el loop infinito del update:
      if (antes.timestampInicio === despues.timestampInicio &&
        antes.cantidadQuimicoPorArea === despues.cantidadQuimicoPorArea &&
        antes.quimicoUtilizado === despues.quimicoUtilizado &&
        antes.recurrente === despues.recurrente) {
        // retornamos nulo porque no tenemos más trabajo que hacer
        // console.log("ES LA MISMA");
        return null;
      }
      // verificarFumigacion retorna promesa:
      return verificarFumigacion(robotId, fumigacionId, despues.timestampInicio)
          .then(() => {
            // si la promesa sale bien, hacemos:
            console.log("Se actualizó la fumigación " + cambios.after.key);
            return Promise.resolve();
            // o ver si retornar nulo porque no tenemos que esperar a nada más
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, restablecemos after ref: " + cambios.after.ref);
            return cambios.after.ref.set(antes);
          });
    });


/** Verifica que no se creen dos fumigaciones para la
 * misma fecha y hora.
 * @param {string} robotId ID del robot
 * @param {string} fumigacionId ID de la fumigación nueva
 * @param {string} tsInicio Fecha y hora de la nueva
 * @return {Promise} retorna promesa
 */
function verificarFumigacion(robotId, fumigacionId, tsInicio) {
  return admin.database().ref("fumigaciones_programadas/" + robotId + "/")
      .once("value").then( (snap) => {
        snap.forEach( (fumigacion) => {
          const tsFumigacion = fumigacion.val().timestampInicio;
          const cmp = tsFumigacion === tsInicio;
          const difFumi = fumigacionId !== fumigacion.key;
          if ( cmp && difFumi ) {
            // Si son iguales, no puedo guardarla
            // console.log("---VERIFICAR FUMIGACION: SON IGUALES---");
            throw new Error("Timestamp repetido");
          } else if (difFumi) {
            // si no son iguales, podemos verificar la brecha temporal
            console.log("Analizando nueva (" + fumigacionId +
                ") contra " + fumigacion.key + "...");
            const evaluacionBrecha =
                evaluarBrechaTemporal(tsInicio, tsFumigacion);
            if (evaluacionBrecha == false) {
              // si hay conflictos con las fechas, salgo
              throw new Error("Conflicto con las brechas temporales");
            }
          }
        });
        return Promise.resolve();
      }).catch((err) => {
        return Promise.reject(err);
      });
}

/** Evalúa una brecha temporal mínima entre las fumigaciones
 @param {string} tsNueva Timestamp de la fumigación a insertar
 @param {string} tsExistente Timestamp de la fumigación contra la que se compara
 @return {Boolean} retorna `true` si está todo ok, caso contrario, `false`
 */
function evaluarBrechaTemporal(tsNueva, tsExistente) {
  const nueva = new Date(parseInt(tsNueva));
  const existente = new Date(parseInt(tsExistente));
  // en principio, vemos si son para el mismo día, o sea, misma fecha:
  const fechaNueva = nueva.getDate() + "-" + (nueva.getMonth()+1) +
    "-" + nueva.getFullYear();
  const fechaExistente = existente.getDate() + "-" + (existente.getMonth()+1) +
    "-" + existente.getFullYear();
  // hacemos la comparación entre fechas
  // console.log("Fecha nueva: " + fechaNueva);
  // console.log("Fecha existente: " + fechaExistente);
  const cmpFechas = fechaNueva === fechaExistente;
  if (cmpFechas == false) {
    // no son en el mismo día, siga siga
    // ver qué pasa cuando son días distintos pero su dif
    // está dentro de la brecha
    return true;
  }
  // si son en el mismo día, tengo que ver la hora
  // la brecha es de 15 minutos
  const brecha = 15 * 60 * 1000;
  const superior = new Date(parseInt(tsNueva) + brecha);
  const inferior = new Date(parseInt(tsNueva) - brecha);
  console.log("-----------------");
  console.log("Brecha superior: " + superior);
  console.log("Brecha inferior: " + inferior);
  console.log("Hora nueva: " + nueva);
  console.log("Hora existente: " + existente);
  // comparamos y verificamos brecha
  const cmpBrecha = (existente < inferior || existente > superior);
  if (cmpBrecha == true) {
    // si brecha es true, significa que está fuera de los límites
    // y se puede insertar la fumigación nueva
    // console.log("Brecha limpia: PROCEDA");
    return true;
  } else {
    // si no, tengo que cancelarla
    console.log("Hay conflictos con la brecha");
    return false;// Promise.reject(new Error("Hay conflictos con la brecha"));
  }
}
