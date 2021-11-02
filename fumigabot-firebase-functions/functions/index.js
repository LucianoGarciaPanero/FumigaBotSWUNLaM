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
            return Promise.resolve("Ok");
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, borramos snapshot ref: " + snapshot.ref);
            return snapshot.ref.remove();
          });
    });

// arreglar después
/* exports.programadaUpdate = functions.database
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
    });*/


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
          // comparo con las programadas que sean de hoy (-12hs) en adelante
          // (12 * cant min en 1 hr * cant seg en 1 min * cant ms en 1 seg)
          const hoy = new Date(Date.now() - (12 * 60 * 60 * 1000));
          if (tsFumigacion >= hoy) {
            // comparo a ver si es el mismo timestamp
            const cmp = tsFumigacion === tsInicio;
            // comparo que sean diferentes ids
            const difFumi = fumigacionId !== fumigacion.key;
            if ( cmp && difFumi ) {
              // Si son iguales, no puedo guardarla
              // throw new Error("Timestamp repetido");
              throw new functions.https.HttpsError("already-exists",
                  "Timestamp repetido");
            } else if (difFumi) {
              // si no son iguales, podemos verificar la brecha temporal
              console.log("Analizando nueva (" + fumigacionId +
                ") contra " + fumigacion.key + "...");
              const evaluacionBrecha =
              evaluarBrechaTemporal(tsInicio, tsFumigacion);
              if (evaluacionBrecha == false) {
                // throw new Error("Conflicto con las brechas temporales");
                throw new functions.https.HttpsError("out-of-range",
                    "Conflicto con las brechas temporales");
              }
            }
          }
        });
        return Promise.resolve("Ok");
      });
}

/** Evalúa una brecha temporal mínima entre las fumigaciones
 @param {string} tsNueva Timestamp de la fumigación a insertar
 @param {string} tsExistente Timestamp de la fumigación contra la que se compara
 @return {Boolean} retorna `true` si está todo ok, caso contrario, `false`
 */
function evaluarBrechaTemporal(tsNueva, tsExistente) {
  // const nueva = new Date(parseInt(tsNueva));
  const existente = new Date(parseInt(tsExistente));
  // la brecha es de 15 minutos
  const brecha = 15 * 60 * 1000;
  const superior = new Date(parseInt(tsNueva) + brecha);
  const inferior = new Date(parseInt(tsNueva) - brecha);
  // console.log("Brecha superior: " + superior);
  // console.log("Brecha inferior: " + inferior);
  // console.log("Hora nueva: " + nueva);
  // console.log("Hora existente: " + existente);
  // console.log("-----------------");
  // comparamos y verificamos brecha
  const cmpBrecha = (existente < inferior || existente > superior);
  return cmpBrecha;
}


exports.evaluarInstantanea = functions.https.onCall((data, context) => {
  // data contiene la data que le pasemos cuando la llamamos desde la app
  // context tiene informacion de autenticacion del user
  // si queremos, podemos retornar un JSON
  const robotId = data.robotId;
  const fumigacionId = data.fumigacionId;
  const tsInicio = data.tsInicio;
  // console.log("onCall - robotId: " + robotId);
  // console.log("onCall - fumiId: " + fumigacionId);
  // console.log("onCall - tsInicio: " + tsInicio);
  return verificarFumigacion(robotId, fumigacionId, tsInicio);
});
