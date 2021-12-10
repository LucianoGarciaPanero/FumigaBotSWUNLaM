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


// ---------------- Triggers ----------------

exports.programadaNueva = functions.database
    .ref("fumigaciones_programadas/{robotId}/{fumigacionId}")
    .onCreate((snapshot, context) => {
      const robotId = context.params.robotId;
      const fumigacionId = context.params.fumigacionId;
      const timestamp = snapshot.val().timestampInicio;

      // verificarFumigacion retorna promesa:
      return verificarFumigacion(robotId, fumigacionId, timestamp)
          .then(() => {
          // programar la ejecución de la fumigación
            return scheduleProgramada(robotId, fumigacionId, snapshot.val());
          })
          .catch((error) => {
            console.log(error);
            console.log("Nope, borramos snapshot ref: " + snapshot.ref);
            snapshot.ref.remove().then(() => {
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


exports.notificarRobot = functions.database
    .ref("robots/{robotId}/detencionAutomatica")
    .onUpdate((cambios, context) => {
      const robotId = context.params.robotId;
      const antes = cambios.before.val();
      const despues = cambios.after.val();

      // si está en false, se supone que lo para la app
      const detAuto = antes == false && despues == true;

      if (detAuto == false) {
        return null;
      }

      return admin.database().ref("robots/" + robotId).once("value")
          .then((robot) => {
            const fumigando = robot.val().fumigando; // ""podría"" no ir
            // lo hacemos xq somo re kpos ;);)
            const razon = robot.val().razonFinalizacion;

            if (fumigando == false) {
              console.log("Razon de detención: " + razon);
              const mensajeFinalizacion = evaluarRazonFinalizacion(razon);

              return detenerFumigacion(robotId, razon).then(()=>{
                return enviarNotificacion(robotId, "Fumigación finalizada",
                    mensajeFinalizacion);
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


// -----------------------------------------

// ---------------- On call ----------------


exports.detenerFumigacion = functions.https.onCall((data, context) => {
  const robotId = data.robotId;
  console.log("ROBOT ID: " + robotId);

  return detenerFumigacion(robotId, "ok");
});

exports.evaluarInstantanea = functions.https.onCall((data, context) => {
  // context tiene informacion de autenticacion del user
  // si queremos, podemos retornar un JSON
  const robotId = data.robotId;
  const fumigacionId = data.fumigacionId;
  const tsInicio = data.tsInicio;
  const quimicoUtilizado = data.quimicoUtilizado;
  return verificarFumigacion(robotId, fumigacionId, tsInicio).then(() => {
    return verificarRecursos(robotId, quimicoUtilizado);
  });
});

exports.programadaDelete = functions.https.onCall((data, context) => {
  const fumigacionId = data.fumigacionId;
  const robotId = data.robotId;

  return borrarProgramada(robotId, fumigacionId);
});


exports.borrarQuimico = functions.https.onCall((data, context) => {
  const robotId = data.robotId;
  const quimico = data.quimico;

  const refRobot = admin.database().ref("robots/" + robotId);
  const refProgramadas = admin.database()
      .ref("fumigaciones_programadas/" + robotId);

  return refRobot.child("quimicosDisponibles").once("value").then((nodo)=> {
    const nuevosQuimicosDisponibles = nodo.val().filter((e) => e != quimico);

    return refRobot.update({quimicosDisponibles: nuevosQuimicosDisponibles})
        .then(()=>{
          return refProgramadas.once("value").then((snap) => {
            snap.forEach((fp) => {
              const eliminada = fp.val().eliminada;
              const cmpQuimico = fp.val().quimicoUtilizado == quimico;
              if (eliminada == false && cmpQuimico == true) {
                return borrarProgramada(robotId, fp.key).then((res)=>{
                  return Promise.resolve(res);
                });
              }
            });
            return Promise.resolve("ok");
          });
        });
  });
});


// -----------------------------------------

// ---------------- On Request ----------------

exports.ejecutarProgramada = functions.https.onRequest((req, res) => {
  const {robotId, fumigacionId, quimicoUtilizado,
    cantArea} = req.body;

  const refFumigacion = admin.database().ref("fumigaciones_programadas/" +
      robotId + "/" + fumigacionId);

  let activa;
  let timestampInicio;
  let recurrente;

  refFumigacion.once("value").then((fp) => {
    // console.log("DAME TODO");
    // console.log(fp.val());
    activa = fp.val().activa;
    // son usados para la creación de la entrada al historial
    timestampInicio = fp.val().timestampInicio;
    recurrente = fp.val().recurrente;
    if (recurrente == true) {
      crearProximaFumigacionRecurrente(robotId, req.body, activa);
    }

    refFumigacion.update({eliminada: true});

    if (activa == false) {
      // si no está activa, no se tiene que ejecutar
      console.log("Ejecutar programada: fumigación desactivada");
      res.status(200).send("OK!");
    } else {
      verificarRecursos(robotId, quimicoUtilizado).then(() => {
        // primero ponemos en fumigando el robot y la detención no automática
        admin.database().ref("robots/" + robotId).update({
          fumigando: true, detencionAutomatica: false}).then(() => {
          admin.database().ref("robots/" + robotId).once("value")
              .then((robot) => {
                refFumigacion.once("value").then((fumigacion) => {
                  iniciarFumigacion(robotId, robot.val(), fumigacion.val())
                      .then(() => {
                        res.status(200).send("OK!");
                      });
                });
              });
        }).catch((err) => {
          console.log("CATCH: poner el robot a fumigar");
          res.status(500).send(err);
        });
      }).catch((err) => {
        console.log("CATCH: verificar recursos");
        console.log(err);

        eliminarTarea(robotId, fumigacionId);

        const mensaje = evaluarRazonFinalizacion(err.message);

        const dataHistorial = {
          timestampInicio: timestampInicio,
          cantidadQuimicoPorArea: cantArea,
          quimicoUtilizado: quimicoUtilizado,
          recurrente: recurrente,
          observaciones: err.message,
        };

        crearEntradaHistorial(robotId, dataHistorial).then(()=>{
          const titulo = "Fumigación programada cancelada";
          enviarNotificacion(robotId, titulo, mensaje).then(() => {
            res.status(500).send(err);
          });
        });
      });
    }
  }).catch((err) => {
    // este es el de la verificacion de los recursos
    console.log("ERROR ejecutar programada:");
    console.log(err.message);

    res.status(500).send(err);
    // 1. eliminar la tarea de la cola
    // 2. hacer la entrada en el historial diciendo el por qué?
  });
});


// -----------------------------------------

// ---------------- Functions ----------------

// ______ Fumigaciones ______

/** Crea la próxima fumigación programada 7 días después de la actual
 * @param {String} robotId ID del robot
 * @param {Object} data Toda la data que necesitamos
 * @param {boolean} activa Si la fumigación programada original está activa o no
*/
function crearProximaFumigacionRecurrente(robotId, data, activa) {
  const {quimicoUtilizado, cantArea,
    timestampInicio} = data;

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
              activa: activa,
              cantidadQuimicoPorArea: obtenerStringCantidadPorArea(cantArea),
              quimicoUtilizado: quimicoUtilizado,
              recurrente: true,
              timestampInicio: timestampInicioNueva.toString(),
              eliminada: false,
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
  let recurrente = fumigacion.recurrente;
  if (recurrente == undefined) {
    recurrente = null;
  }

  return admin.database().ref("robots/" + robotId + "/fumigacionActual")
      .set({
        timestampInicio: fumigacion.timestampInicio,
        quimicoUtilizado: fumigacion.quimicoUtilizado,
        cantidadQuimicoPorArea:
          obtenerValorNumerico(fumigacion.cantidadQuimicoPorArea),
        nivelQuimicoInicial: robot.nivelQuimico,
        nivelBateriaInicial: robot.bateria,
        recurrente: recurrente,
      });
}


/** Crea un nuevo nodo en fumigaciones_historial.
 *@param {String} robotId ID del robot
 *@param {Object} fumigacion datos de la fumigación
 *@return {Promise} retorna promesa
*/
function crearEntradaHistorial(robotId, fumigacion) {
  const ref = admin.database().ref("fumigaciones_historial/" + robotId);

  let idHistorial = 1;

  let timestampFin = fumigacion.timestampFin;
  if (timestampFin == null || timestampFin == undefined) {
    timestampFin = "0";
  }

  let nivelQuimicoInicial = fumigacion.nivelQuimicoInicial;
  if (nivelQuimicoInicial == null || nivelQuimicoInicial == undefined) {
    nivelQuimicoInicial = 0;
  }

  let nivelQuimicoFinal = fumigacion.nivelQuimicoFinal;
  if (nivelQuimicoFinal == null || nivelQuimicoFinal == undefined) {
    nivelQuimicoFinal = 0;
  }

  let nivelBateriaInicial = fumigacion.nivelBateriaInicial;
  if (nivelBateriaInicial == null || nivelBateriaInicial == undefined) {
    nivelBateriaInicial = 0;
  }

  let nivelBateriaFinal = fumigacion.nivelBateriaFinal;
  if (nivelBateriaFinal == null || nivelBateriaFinal == undefined) {
    nivelBateriaFinal = 0;
  }

  return ref.once("value").then((snap) => {
    snap.forEach((fh) => {
      idHistorial++;
    });

    ref.child("fh" + idHistorial).set({
      timestampInicio: fumigacion.timestampInicio,
      timestampFin: timestampFin,
      quimicoUtilizado: fumigacion.quimicoUtilizado,
      cantidadQuimicoPorArea:
        obtenerStringCantidadPorArea(
            fumigacion.cantidadQuimicoPorArea),
      nivelQuimicoInicial: nivelQuimicoInicial,
      nivelQuimicoFinal: nivelQuimicoFinal,
      nivelBateriaInicial: nivelBateriaInicial,
      nivelBateriaFinal: nivelBateriaFinal,
      observaciones: fumigacion.observaciones,
      recurrente: fumigacion.recurrente,
    });
  });
}


/** Elimina la tarea de la cola en caso de ser necesario.
 * @param {String} robotId ID del robot que va a fumigar.
 * @param {String} fumigacionId ID de la fumigación a eliminar su tarea.
 * @return {Promise} retorna promesa
*/
function eliminarTarea(robotId, fumigacionId) {
  const ref = admin.database().ref("fumigaciones_programadas/" + robotId +
  "/" + fumigacionId);

  return ref.once("value").then((fumigacion) => {
    const taskId = fumigacion.val().taskId;
    tasksClient.deleteTask({name: taskId}).then(() => {
      return Promise.resolve("OK");
    }).catch((error)=>{
      console.log("catch delete task");
      console.log(error);
    });
  });
}

/** Crea las tareas para la ejecución de las fumigaciones programadas.
 * @param {String} robotId ID del robot que comienza a fumigar
 * @param {String} fumigacionId ID de la fumigación programada
 * @param {Object} fumigacion Fumigación programada a comenzar
 * @return {Promise} retorna promesa
 */
function scheduleProgramada(robotId, fumigacionId, fumigacion) {
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

  const request = {parent: queuePath, task: task};
  return tasksClient.createTask(request).then(([taskCreada], error) => {
    const taskId = taskCreada.name;
    // console.log("TaskID: " + taskId);
    return admin.database().ref("fumigaciones_programadas/" + robotId + "/" +
      fumigacionId).update({taskId: taskId}).catch((error) => {
      console.log("Error update taskId");
      console.log(error);
    });
  }).catch((err) => {
    console.log("catch create task:");
    console.log(err);
    throw new functions.https.HttpsError("unknown", err);
  });
}


/** Detiene la fumigación actual
 * @param {String} robotId ID del robot a detener
 * @param {String} observaciones observaciones respecto a la detención
 * @return {Promise} retorna promesa
 */
function detenerFumigacion(robotId, observaciones) {
  let fumigacionActual;
  let bateria;
  let nivelQuimico;
  let recurrente;
  // let idHistorial = 1;

  // const ref = admin.database().ref("fumigaciones_historial/" + robotId);

  return admin.database().ref("robots/" + robotId)
      .update({fumigando: false}).then(() => {
        admin.database().ref("robots/" + robotId).once("value")
            .then((robot) => {
              fumigacionActual = robot.val().fumigacionActual;
              bateria = robot.val().bateria;
              nivelQuimico = robot.val().nivelQuimico;

              recurrente = fumigacionActual.recurrente;
              if (recurrente == undefined) {
                recurrente = null;
              }

              const dataHistorial = {
                timestampInicio: fumigacionActual.timestampInicio,
                timestampFin: Date.now().toString(),
                quimicoUtilizado: fumigacionActual.quimicoUtilizado,
                cantidadQuimicoPorArea: fumigacionActual.cantidadQuimicoPorArea,
                nivelQuimicoInicial: fumigacionActual.nivelQuimicoInicial,
                nivelQuimicoFinal: nivelQuimico,
                nivelBateriaInicial: fumigacionActual.nivelBateriaInicial,
                nivelBateriaFinal: bateria,
                observaciones: observaciones,
                recurrente: recurrente,
              };

              crearEntradaHistorial(robotId, dataHistorial);

              /* ref.once("value").then((snap) => {
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
              });*/
            });
      });
}

/** Borra la tarea de la fumigación programada y le setea el
 * atributo "eliminada" en `true`.
 * @param {String} robotId ID del robot
 * @param {String} fumigacionId ID de la fumigación programada a borrar
 * @return {Promise} retorna promesa
 */
function borrarProgramada(robotId, fumigacionId) {
  return eliminarTarea(robotId, fumigacionId).then(() => {
    return admin.database().ref("fumigaciones_programadas/" +
    robotId + "/" + fumigacionId).update({eliminada: true}).then(()=>{
      return "ok";
    });
  });
}


// ______ Verificaciones ______

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
  const cmpBrecha = (existente < inferior || existente > superior);
  return cmpBrecha;
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

        if (bateria <= MINIMO_BATERIA) {
          throw new functions.https.HttpsError("out-of-range",
              "bnd");
        } else if (quimico <= MINIMO_QUIMICO) {
          throw new functions.https.HttpsError("out-of-range",
              "qnd");
        } else if (fumigando == true) {
          throw new functions.https.HttpsError("unavailable",
              "rf");
        } else if (encendido == false) {
          throw new functions.https.HttpsError("unavailable",
              "ra");
        } else if (ultimoQuimico != quimicoUtilizado) {
          throw new functions.https.HttpsError("invalid-argument",
              "qnc");
        }
        return Promise.resolve("ok");
      });
}


// _____ Notificaciones ______

/** Envía notificaciones con el título y mensaje correspondiente.
 * @param {String} robotId ID del robot que quiere notificar.
 * @param {String} titulo Título de la notificación.
 * @param {String} mensaje Mensaje a mostrar.
 * @return {Promise} retorna promesa.
 */
function enviarNotificacion(robotId, titulo, mensaje) {
  let token;
  return admin.database().ref("users/" + robotId).once("value")
      .then((robot) => {
        robot.forEach((user) => {
          token = user.val();
          // console.log("Token: " + token);
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
          });
        });
      }).catch((err) => {
        console.log("ERROR MENSAJE");
        console.log(err);
      });
}


// _____ Utility ______

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


/** Evalúa las razones por la cual se detuvo el robot
 * y la envía en una notificación a la app del usuario.
 * @param {String} razon Razón por la cual se detuvo el robot.
 * @return {String} retorna la razón de finalización xd.
*/
function evaluarRazonFinalizacion(razon) {
  let mensaje = "";

  switch (razon.toLowerCase()) {
    case "ok":
      mensaje = "El robot terminó de fumigar";
      break;
    case "fdb":
      mensaje = "El robot se detuvo por falta de batería";
      break;
    case "fdq":
      mensaje = "El robot se detuvo por falta de químico";
      break;
    case "bnd": // batería no disponible
      mensaje = "No hay suficiente batería para fumigar";
      break;
    case "qnd":
      mensaje = "No hay suficiente químico para fumigar";
      break;
    case "qnc":
      mensaje = "El químico que contiene el robot no coincide con el de " +
      "la fumigación programada";
      break;
    case "rf":
      mensaje = "El robot se encuentra ejecutando una fumigación previa";
      break;
    case "ra":
      mensaje = "El robot está apagado";
      break;
    default:
      break;
  }

  return mensaje;
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
