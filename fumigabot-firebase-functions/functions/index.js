const functions = require("firebase-functions");
const admin = require("firebase-admin");
const {CloudTasksClient} = require("@google-cloud/tasks");
// --- Constantes para verificar recursos ---
const MINIMO_BATERIA = 5;
const MINIMO_QUIMICO = 5;
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
      // const cantArea = snapshot.val().cantidadQuimicoPorArea;
      // const quimicoUtilizado = snapshot.val().quimicoUtilizado;

      // verificarFumigacion retorna promesa:
      return verificarFumigacion(robotId, fumigacionId, timestamp)
          .then(() => {
          // programar la ejecución de la fumigación
            return scheduleProgramada(robotId, fumigacionId, snapshot.val());
            // fumigacionId, quimicoUtilizado,
            // cantArea, timestamp);
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, borramos snapshot ref: " + snapshot.ref);
            snapshot.ref.remove().then(() => {
              // return Promise.reject("Fumigación borrada");
              throw new functions.https.HttpsError("aborted",
                  "Fumigación borrada");
            });
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
        // && antes.taskId === despues.taskId) {
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
            tasksClient.deleteTask({name: antesTask}).then(() => {
              // if (despues.activa == "true") {
              return scheduleProgramada(robotId, fumigacionId,
                  despues);
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
      .once("value").then((snap) => {
        snap.forEach((fumigacion) => {
          const tsFumigacion = fumigacion.val().timestampInicio;
          // comparo con las programadas que sean de hoy (-12hs) en adelante
          // (12 * cant min en 1 hr * cant seg en 1 min * cant ms en 1 seg)
          const hoy = new Date(Date.now() - (12 * 60 * 60 * 1000));
          if (tsFumigacion >= hoy) {
          // comparo a ver si es el mismo timestamp
            const cmp = tsFumigacion === tsInicio;
            // comparo que sean diferentes ids
            const difFumi = fumigacionId !== fumigacion.key;
            if (cmp && difFumi) {
              throw new functions.https.HttpsError("already-exists",
                  "Timestamp repetido");
            } else if (difFumi) {
            // si no son iguales, podemos verificar la brecha temporal
            // console.log("Analizando nueva (" + fumigacionId +
            // ") contra " + fumigacion.key + "...");
              evaluarBrechaTemporal(tsInicio, tsFumigacion);
              /* const evaluacionBrecha =
              evaluarBrechaTemporal(tsInicio, tsFumigacion);
               if (evaluacionBrecha == false) {
                throw new functions.https.HttpsError("out-of-range",
                    "Conflicto con las brechas temporales");
              }*/
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
  const quimicoUtilizado = data.quimicoUtilizado;
  console.log("Quimico utilizado: " + quimicoUtilizado);
  return verificarFumigacion(robotId, fumigacionId, tsInicio).then(() => {
    return verificarRecursos(robotId, quimicoUtilizado);
  });
});


/** Crea las tareas para la ejecución de las fumigaciones programadas.
 * @param {String} robotId ID del robot que comienza a fumigar
 * @param {String} fumigacionId ID de la fumigación programada
 * @param {Object} fumigacion Fumigación programada a comenzar
 * @return {Promise} retorna promesa
 */
function scheduleProgramada(robotId, fumigacionId, fumigacion) {
  // fumigacionId, quimicoUtilizado,
  //  cantArea, timestamp) {
  const cantArea = obtenerValorNumerico(fumigacion.cantidadQuimicoPorArea);
  const payload = {
    robotId: robotId,
    fumigacionId: fumigacionId,
    quimicoUtilizado: fumigacion.quimicoUtilizado,
    cantArea: cantArea,
    timestampInicio: fumigacion.timestampInicio,
    recurrente: fumigacion.recurrente,
  };
  const ts = Number.parseInt(fumigacion.timestampInicio) / 1000;

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
  return tasksClient.createTask(request).then(([taskCreada], error) => {
    const taskId = taskCreada.name;
    // console.log("TaskID: " + taskId);
    return admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
      fumigacionId).update({taskId: taskId});
  }).catch((err) => {
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
  const {robotId, fumigacionId, quimicoUtilizado,
    cantArea, recurrente} = req.body;

  /* 1. si es recurrente, crear la de la próxima semana o recurrencia
     2. independientemente del punto 1, ejecutar la actual (el código original)
  */

  if (recurrente == true) {
    crearProximaFumigacionRecurrente(robotId, req.body);
  }

  admin.database().ref("fumigaciones_programadas/" +
    robotId + "/" + fumigacionId).once("value").then((fp) => {
    if (fp.val().activa == false) {
      res.status(200).send("OK!");
    }
  });

  verificarRecursos(robotId, quimicoUtilizado).then(() => {
    // primero ponemos en fumigando el robot y la cantidad por área
    admin.database().ref("robots/" + robotId).update({
      fumigando: true, cantidadQuimicoPorArea: cantArea})
        .then(() => {
        // vamos a ver si es recurrente o no
          admin.database().ref("fumigaciones_programadas/" +
          robotId + "/" + fumigacionId).once("value").then((fumigacion) => {
            if (fumigacion.val().recurrente == false) {
            // si no es recurrente, la desactivamos
              console.log("entra a recurrente = false");
              fumigacion.ref.update({activa: false}).then(() => {
                // res.status(200).send("OK!");
              }).catch((err) => {
                throw new Error(err);
              });
            } else {
              // robar los días de recurrencia para agregarlos
              // a fumigacionActual
              // diasRecurrencia = algo;
              console.log("recurrente != false");
            }
            admin.database().ref("robots/" + robotId).once("value")
                .then((robot) => {
                  iniciarFumigacion(robotId, robot.val(), fumigacion.val())
                      .then(() => {
                        res.status(200).send("OK!");
                      });
                });
          }).catch((err) => {
            res.status(500).send(err);
          });
        }).catch((err) => {
          console.log(err);
          res.status(500).send(err);
        });
  }).catch((err) => {
    // este es el de la verificacion de los recursos
    console.log("ERROR ejecutar programada:");
    console.log(err.message);
    // 1. eliminar la tarea de la cola
    // 2. hacer la entrada en el historial diciendo el por qué?
    eliminarTarea(robotId, fumigacionId);
    const titulo = "Fumigación programada cancelada";
    enviarNotificacion(titulo, err.message).then(() => {
      res.status(500).send(err);
    });
  });
});


/** Crea la próxima fumigación programada 7 días después de la actual
 * @param {String} robotId ID del robot
 * @param {Object} data Toda la data que necesitamos
 * (body de ejecutarProgramada)
*/
function crearProximaFumigacionRecurrente(robotId, data) {
  const {quimicoUtilizado, cantArea,
    timestampInicio} = data;

  console.log("crear proxima fumigacion recurrente");
  console.log(robotId);

  let idProgramada = 1;
  const timestampInicioNueva = parseInt(timestampInicio) +
    (7 * 24 * 60 * 60 * 1000);

  admin.database().ref("fumigaciones_programadas/" + robotId)
      .once("value").then((snap) => {
        snap.forEach((fp) => {
          idProgramada++;
        });
        console.log("Id programada");
        console.log(idProgramada);
        // crear el nodo de la programada
        admin.database().ref("fumigaciones_programadas/" + robotId)
            .child("fp" + idProgramada).set({
              activa: true,
              cantidadQuimicoPorArea: obtenerStringCantidadPorArea(cantArea),
              quimicoUtilizado: quimicoUtilizado,
              recurrente: true,
              timestampInicio: timestampInicioNueva.toString(),
            });
      });
}

/** Crea la estructura para fumigacionActual
 * @param {String} robotId ID del robot
 * @param {Object} robot Robot completo
 * @param {Object} fumigacion nodo completo de la fumigación (programada)
 * @return {Promise} retorna promesa
*/
function iniciarFumigacion(robotId, robot, fumigacion) {
  console.log(robot);
  console.log("------");
  console.log(fumigacion);
  return admin.database().ref("robots/" + robotId + "/fumigacionActual")
      .set({
        timestampInicio: fumigacion.timestampInicio,
        quimicoUtilizado: fumigacion.quimicoUtilizado,
        cantidadQuimicoPorArea:
          obtenerValorNumerico(fumigacion.cantidadQuimicoPorArea),
        nivelQuimicoInicial: robot.nivelQuimico,
        nivelBateriaInicial: robot.bateria,
      });
}


/** Elimina la tarea de la cola en caso de ser necesario.
 * @param {String} robotId ID del robot que va a fumigar.
 * @param {String} fumigacionId ID de la fumigación a eliminar su tarea.
 * @return {Promise} retorna promesa
*/
function eliminarTarea(robotId, fumigacionId) {
  return admin.database().ref("fumigaciones_programadas/" + robotId +
  "/" + fumigacionId).once("value").then((fumigacion) => {
    const taskId = fumigacion.val().taskId;
    tasksClient.deleteTask({name: taskId}).then(() => {
      return Promise.resolve("OK");
    });
  });
}


/** Chequea antes de empezar una fumigación si se cuentan con los
 * recursos mínimos.
 * @param {String} robotId ID del robot a consultar sus recursos.
 * @param {String} quimicoUtilizado químico a usar en la fumigación.
 * @return {Promise} retorna promesa con los detalles correspondientes.
 * */
function verificarRecursos(robotId, quimicoUtilizado) {
  return admin.database().ref("robots/" + robotId).once("value")
      .then((robot) => {
        const bateria = robot.val().bateria;
        const quimico = robot.val().nivelQuimico;
        const fumigando = robot.val().fumigando;
        const encendido = robot.val().encendido;
        const ultimoQuimico = robot.val().ultimoQuimico;

        /* if (bateria > MINIMO_BATERIA && quimico > MINIMO_QUIMICO &&
          fumigando == false && encendido == true) {
          resultado = true;
        }
        resultado = false;*/
        // let mensaje = "ok";
        if (bateria <= MINIMO_BATERIA) {
          // mensaje = "No hay suficiente batería";
          throw new functions.https.HttpsError("out-of-range",
              "No hay suficiente batería");
        } else if (quimico <= MINIMO_QUIMICO) {
          // mensaje = "No hay suficiente químico";
          throw new functions.https.HttpsError("out-of-range",
              "No hay suficiente químico");
        } else if (fumigando == true) {
          // mensaje = "El robot ya se encuentra fumigando";
          throw new functions.https.HttpsError("unavailable",
              "El robot ya se encuentra fumigando");
        } else if (encendido == false) {
          // mensaje = "El robot está apagado";
          throw new functions.https.HttpsError("unavailable",
              "El robot está apagado");
        } else if (ultimoQuimico != quimicoUtilizado) {
          // mensaje = "El último químico utilizado no coincide con el de " +
          //  "la fumigación programada";
          throw new functions.https.HttpsError("invalid-argument",
              "El último químico utilizado no coincide con el de " +
            "la fumigación programada");
        }
        return Promise.resolve("ok");
        /* if (mensaje == "ok") {
          return Promise.resolve("Ok");
        } else {
          return enviarNotificacion(titulo, mensaje);
        }*/
      });
}


// ------- Robot -------

/** Envía notificaciones con el título y mensaje correspondiente.
 * @param {String} titulo Título de la notificación.
 * @param {String} mensaje Mensaje a mostrar.
 * @return {Promise} retorna promesa.
 */
function enviarNotificacion(titulo, mensaje) {
  let token;
  return admin.database().ref("robots/0/").once("value").then((robot) => {
    token = robot.val().token;
    console.log("Token: " + token);

    const payload = {
      token: token,
      notification: {
        title: titulo,
        body: mensaje,
      },
      data: {body: mensaje},
    };

    return admin.messaging().send(payload).then((res) => {
      console.log("MENSAJE OK, ID MENSAJE: " + res);
    }).catch((err) => {
      console.log("ERROR MENSAJE");
      console.log(err);
    });
  });
}

/** Evalúa las razones por la cual se detuvo el robot
 * y la envía en una notificación a la app del usuario.
 * @param {String} razon Razón por la cual se detuvo el robot.
 * @return {Promesa} retorna promesa.
*/
function evaluarDetencionAutomatica(razon) {
  let mensaje = "";

  if (razon == "ok") {
    mensaje = "El robot terminó de fumigar";
  } else if (razon == "fdq") {
    mensaje = "El robot se detuvo por falta de químico";
  } else if (razon == "fdb") {
    mensaje = "El robot se detuvo por falta de batería";
  }

  return enviarNotificacion("Fumigación finalizada", mensaje);
}

exports.notificarRobot = functions.database
    .ref("robots/{robotId}/detencionAutomatica")
    .onUpdate((cambios, context) => {
      const robotId = context.params.robotId;
      const antes = cambios.before.val();
      const despues = cambios.after.val();

      // si está en false, se supone que lo para la app
      const detAuto = antes == false && despues == true;
      // antes.detencionAutomatica == false &&
      // despues.detencionAutomatica == true;

      if (detAuto == false) {
        return null;
      }

      admin.database().ref("robots/" + robotId).once("value").then((robot) => {
        const fumigando = robot.val().fumigando; // podría no ir
        const razon = robot.val().razonFinalizacion;

        if (fumigando == false) {
          console.log("Razon de detención: " + razon);
          detenerFumigacion(robotId).then(()=>{
            return evaluarDetencionAutomatica(razon.toLowerCase());
          });
        }
      });
      /* const stopFumigando = antes.fumigando == true &&
        despues.fumigando == false;
      const notificar = stopFumigando && detAuto;*/

      /* const quimico = despues.nivelQuimico;
      const bateria = despues.bateria;
      const stopQuimico = quimico < MINIMO_QUIMICO;
      const stopBat = bateria < MINIMO_BATERIA;

      if (stopQuimico) {
        return evaluarDetencionAutomatica("fdq");
      } else if (stopBat) {
        return evaluarDetencionAutomatica("fdb");
      }*/
      /* if (notificar) {
        // return evaluarDetencionAutomatica("ok");
        const razon = despues.razonFinalizacion;
        console.log("Razon de detención: " + razon);
        return evaluarDetencionAutomatica(razon);
      }*/
    });


/** Tenemos que pasarle:
     *  - idRobot
     *
    */
exports.detenerFumigacion = functions.https.onCall((data, context) => {
  const robotId = data.robotId;
  console.log("ROBOT ID: " + robotId);

  return detenerFumigacion(robotId);
});

/** Detiene la fumigación actual
 * @param {String} robotId ID del robot a detener
 * @return {Promise} retorna promesa
 */
function detenerFumigacion(robotId) {
  // tenemos que:
  /*
    - Dejar de fumigar
    - Tomar valor bateria
    - Tomar valor quimico
    - Tomar fecha hora actual (timestamp fin)
    - Pasar fumigacionActual a Historial
  */
  let fumigacionActual;
  let bateria;
  let nivelQuimico;
  let idHistorial = 1;

  const ref = admin.database().ref("fumigaciones_historial/" + robotId);

  return admin.database().ref("robots/" + robotId)
      .update({fumigando: false}).then(() => {
        admin.database().ref("robots/" + robotId).once("value")
            .then((robot) => {
              fumigacionActual = robot.val().fumigacionActual;
              bateria = robot.val().bateria;
              nivelQuimico = robot.val().nivelQuimico;

              ref.once("value").then((snap) => {
                snap.forEach((fh) => {
                  idHistorial++;
                });

                console.log("ID HISTORIAL");
                console.log(idHistorial);
                const timestampFin = Date.now().toString();
                ref.child("fh" + idHistorial).set({
                  timestampInicio: fumigacionActual.timestampInicio,
                  timestampFin: timestampFin,
                  quimicoUtilizado: fumigacionActual.quimicoUtilizado,
                  cantidadQuimicoPorArea:
                    obtenerStringCantidadPorArea(
                        fumigacionActual.cantidadQuimicoPorArea),
                  nivelQuimicoInicial: fumigacionActual.nivelQuimicoInicial,
                  nivelQuimicoFinal: nivelQuimico,
                  nivelBateriaInicial: fumigacionActual.nivelBateriaInicial,
                  nivelBateriaFinal: bateria,
                });
              });
            });
      });
}

/** Obtener cadena de cantidad de texto por área.
 * @param {Number} cantArea numerito del robot
 * @return {String} cadena con nombre completo
 * */
function obtenerStringCantidadPorArea(cantArea) {
  if (cantArea == 1) {
    return "Baja - Ráfaga de 0,5 segundos";
  } else if (cantArea == 2) {
    return "Media - Ráfaga de 1 segundo";
  }
  return "Alta - Ráfaga de 2 segundos";
}
