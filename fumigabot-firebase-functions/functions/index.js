const functions = require("firebase-functions");
const admin = require("firebase-admin");
const {CloudTasksClient} = require("@google-cloud/tasks");
// --- Configurar Tasks ---
const tasksClient = new CloudTasksClient();
let queuePath = "";
let url = "";
configurarTasks();
admin.initializeApp();


/** Hace la configuración inicial para las tareas
 */
function configurarTasks() {
  // Get the project ID from the FIREBASE_CONFIG env var
  const project = JSON.parse(process.env.FIREBASE_CONFIG).projectId;
  const location = "us-central1";
  const queue = "programadasQueue";

  queuePath = tasksClient.queuePath(project, location, queue);

  url = `https://${location}-${project}.cloudfunctions.net/ejecutarProgramada`;
  // url = "https://localhost:5001/fumigabot/us-central1/ejecutarProgramada";
}

exports.programadaNueva = functions.database
    .ref("fumigaciones_programadas/{robotId}/{fumigacionId}")
    .onCreate((snapshot, context) => {
      const robotId = context.params.robotId;
      const fumigacionId = context.params.fumigacionId;
      const timestamp = snapshot.val().timestampInicio;
      const cantArea = snapshot.val().cantidadQuimicoPorArea;
      // verificarFumigacion retorna promesa:
      return verificarFumigacion(robotId, fumigacionId, timestamp)
          .then(() => {
            // programar la ejecución de la fumigación
            return scheduleProgramada(robotId, fumigacionId,
                cantArea, timestamp);
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
      // verificar la fumigacion
      return verificarFumigacion(robotId, fumigacionId, despues.timestampInicio)
          .then(() => {
            // ver si la fumigación de ANTES ya tenía creada una tarea:
            // me falta setearle el campo "activa" así que hardcodeo
            // if (antes.activa == "true") {
            const antesTask = antes.taskId;
            tasksClient.deleteTask({name: antesTask}).then(()=>{
              // if (despues.activa == "true") {
              return scheduleProgramada(robotId, fumigacionId,
                  despues.cantidadQuimicoPorArea,
                  despues.timestampInicio);
              // }
            });
            // }
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
          // comparo con las programadas que sean de hoy (-12hs) en adelante
          // (12 * cant min en 1 hr * cant seg en 1 min * cant ms en 1 seg)
          const hoy = new Date(Date.now() - (12 * 60 * 60 * 1000));
          if (tsFumigacion >= hoy) {
            // comparo a ver si es el mismo timestamp
            const cmp = tsFumigacion === tsInicio;
            // comparo que sean diferentes ids
            const difFumi = fumigacionId !== fumigacion.key;
            if ( cmp && difFumi ) {
              throw new functions.https.HttpsError("already-exists",
                  "Timestamp repetido");
            } else if (difFumi) {
              // si no son iguales, podemos verificar la brecha temporal
              // console.log("Analizando nueva (" + fumigacionId +
              // ") contra " + fumigacion.key + "...");
              const evaluacionBrecha =
              evaluarBrechaTemporal(tsInicio, tsFumigacion);
              if (evaluacionBrecha == false) {
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
  const existente = new Date(parseInt(tsExistente));
  // la brecha es de 15 minutos
  const brecha = 15 * 60 * 1000;
  const superior = new Date(parseInt(tsNueva) + brecha);
  const inferior = new Date(parseInt(tsNueva) - brecha);
  // comparamos y verificamos brecha
  const cmpBrecha = (existente < inferior || existente > superior);
  return cmpBrecha;
}


exports.evaluarInstantanea = functions.https.onCall((data, context) => {
  // dejo esto escrito acá para que sea útil en un futuro:
  // data contiene la data que le pasemos cuando la llamamos desde la app
  // context tiene informacion de autenticacion del user
  // si queremos, podemos retornar un JSON
  const robotId = data.robotId;
  const fumigacionId = data.fumigacionId;
  const tsInicio = data.tsInicio;
  return verificarFumigacion(robotId, fumigacionId, tsInicio);
});


/** Crea las tareas para la ejecución de las fumigaciones programadas.
 * @param {String} robotId ID del robot que comienza a fumigar
 * @param {String} fumigacionId ID de la fumigación programada a comenzar
 * @param {String} cantArea cantidad de químico por área de la fumigación
 * @param {String} timestamp timestamp de dicha fumigación
 * @return {Promise} retorna promesa
 */
function scheduleProgramada(robotId, fumigacionId, cantArea, timestamp) {
  cantArea = obtenerValorNumerico(cantArea);
  const payload = {robotId: robotId,
    fumigacionId: fumigacionId,
    cantArea: cantArea};
  const ts = Number.parseInt(timestamp) / 1000;

  // le decimos a la tarea que, en ese timestamp seteado,
  // haga un post a nuestra función de "ejecutar programada (está en la URL)"
  const task = {
    httpRequest: {
      httpMethod: "POST",
      url,
      headers: {"Content-Type": "application/json"},
      body: Buffer.from(JSON.stringify(payload)).toString("base64"),
    },
    scheduleTime: {
      seconds: ts, // acá poner timestamp de fumigación
    },
  };

  // console.log("CREATE TASK");
  const request = {parent: queuePath, task: task};
  return tasksClient.createTask(request).then( ([taskCreada], error) => {
    const taskId = taskCreada.name;
    // console.log("TaskID: " + taskId);
    return admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
    fumigacionId).update({taskId: taskId});
  }).catch( (err) => {
    console.log("catch create task:");
    console.log(err);
    throw new functions.https.HttpsError("unknown", err);
  });
}

/** Retorna el valor numérico de la cadena que representa a la cantidad
 * de químico por área.
 * @param {String} cantArea es la cantidad de químico por área en formato texto.
 * @return {Number} `1`: cantidad de químico baja.
 * `2`: cantidad de químico media.
 * `3`: cantidad de químico alta.
*/
function obtenerValorNumerico(cantArea) {
  if (cantArea.includes("Baja")) {
    return 1;
  } else if (cantArea.includes("Media")) {
    return 2;
  }
  return 3;
}

exports.ejecutarProgramada = functions.https.onRequest((req, res) => {
  const {robotId, fumigacionId, cantArea} = req.body;
  // console.log("Robot ID: " + robotId + " | Fumigacion ID: " + fumigacionId +
  //    " Cant x area: " + cantArea);
  try {
    // primero ponemos en fumigando el robot y la cantidad por área
    admin.database().ref("robots/" + robotId).update({fumigando: true,
      cantidadQuimicoPorArea: cantArea}).then(()=>{
      // vamos a ver si es recurrente o no
      admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
      fumigacionId).once("value").then((fumigacion) => {
        if (fumigacion.val().recurrente == true) {
          res.status(200).send("OK!");
        } else {
          // si no es recurrente, la desactivamos
          fumigacion.ref.update({activa: false}).then(()=>{
            res.status(200).send("OK!");
          }).catch((err)=>{
            res.status(500).send(err);
          });
        }
      }).catch((err) => {
        res.status(500).send(err);
      });
    }).catch((err) => {
      res.status(500).send(err);
    });
  } catch (err) {
    console.log("ERROR ejecutar programada:");
    console.log(err);
    res.status(500).send(err);
  }
});
