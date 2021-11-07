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

  // console.log("----- INIT -------");
  // console.log("Project: " + project);

  queuePath = tasksClient.queuePath(project, location, queue);
  // console.log("QueuePath: " + queuePath);

  url = `https://${location}-${project}.cloudfunctions.net/ejecutarProgramada`;
  // url = "https://localhost:5001/fumigabot/us-central1/ejecutarProgramada";
  // console.log("URL: " + url);
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
            // si la promesa sale bien, hacemos:
            console.log("Se agregó la fumigación " + snapshot.key);
            // programar la ejecución de la fumigación
            return scheduleProgramada(robotId, fumigacionId,
                cantArea, timestamp);
            // return Promise.resolve("Ok");
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
            console.log(" ---- FUMIGACION NUEVA:");
            console.log(despues);
            // ver si la fumigacion de before ya tenía creada una tarea:
            if (antes.activa == "true") {
              return tasksClient.deleteTask({name: antes.taskId})
                  .then(()=>{
                    if (despues.activa == "true") {
                      return scheduleProgramada(robotId, fumigacionId,
                          despues.cantidadQuimicoPorArea,
                          despues.timestampInicio);
                    }
                  });
            }

            // si la promesa sale bien, hacemos:
            console.log("Se actualizó la fumigación " + cambios.after.key);
            // programar la ejecución de la fumigación
            // return scheduleProgramada(robotId, fumigacionId,
            // despues.timestampInicio);
            // return Promise.resolve("Ok");
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, restablecemos after ref: " + cambios.after.ref);
            return cambios.after.ref.set(antes);
          });
      /* return verificarFumigacion(robotId, fumigacionId,
      despues.timestampInicio)
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
          });*/
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
              // Si son iguales, no puedo guardarla
              throw new functions.https.HttpsError("already-exists",
                  "Timestamp repetido");
            } else if (difFumi) {
              // si no son iguales, podemos verificar la brecha temporal
              console.log("Analizando nueva (" + fumigacionId +
                ") contra " + fumigacion.key + "...");
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
  // const docPath = snapshot.ref.path
  const payload = {robotId: robotId,
    fumigacionId: fumigacionId,
    cantArea: cantArea}; // { docPath }
  // console.log("------------------");
  console.log("PAYLOAD recien armado:");
  console.log(payload);
  // console.log("        ");
  const ts = Number.parseInt(timestamp) / 1000;

  // le decimos que a ese timestamp haga un post a nuestra funcion
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

  // encolamos la task en nuestra queue
  // const [ response ] = await tasksClient
  // .createTask({ parent: queuePath, task })

  console.log("CREATE TASK");
  const request = {parent: queuePath, task: task};
  console.log(request);
  return tasksClient.createTask(request).then( ([taskCreada], error) => {
    // console.log("TASK CREADA");
    // console.log(taskCreada);
    const taskId = taskCreada.name;
    // console.log("TaskID: " + taskId);
    return admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
    fumigacionId).update({taskId: taskId});
    // return Promise.resolve("Ok crear task");
    // return ref.update({ taskId : taskId })
    // .then(()=>{return Promise.resolve("Ok")});
  }).catch( (err) => {
    console.log("catch create task:");
    console.log(err);
    throw new functions.https.HttpsError("unknown", err);
    // return Promise.reject(err);
  });
  /* const taskId = response.name;
  console.log("TASK ID: " + taskId); */
}

exports.ejecutarProgramada = functions.https.onRequest((req, res) => {
  const {robotId, fumigacionId, cantArea} = req.body;
  console.log("Robot ID: " + robotId + " | Fumigacion ID: " + fumigacionId +
      "Cant x area: " + cantArea);
  try {
    /* await admin.database().ref(payload).update( {} );
    // setear el robot en fumigando true
    await admin.database().ref("robots/{robotId}")
      .update( {fumigando: "true" });
    // setear el taskid a la fumigación
    // ...
    // si no es recurrente, setear activa en false: podemos traerlo del payload
    if(payload.recurrente == "false") {
      await admin.database()
        .ref("fumigaciones_programadas/{robotId}/{fumigacionId}")
        .update( {activa: "false"} );
    }*/
    // probamos con esto a ver si hace algo
    admin.database().ref("robots/" + robotId).update({fumigando: true})
        .then(()=>{
          admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
          fumigacionId).child("recurrente").once("value")
              .then((valorRecurrente) => {
                console.log("valor recurrente.val: " + valorRecurrente.val());
                if (valorRecurrente.val() == "false") {
                  console.log("Ref de valor recurrente:");
                  console.log(valorRecurrente.ref);
                  valorRecurrente.ref.update({activa: false});
                }
                res.status(200).send("OK!");
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
