package com.example.fumigabot.home;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.fumigabot.ConfigurarRobotActivity;
import com.example.fumigabot.NuevaFumigacionActivity;
import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class InicioFragment extends Fragment {

    public static InicioFragment Instance;

    private FirebaseFunctions firebaseFunctions;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceFumigando;
    private DatabaseReference referenceFumigacion;
    private DatabaseReference refProgramadas;
    private MaterialCardView cardEstadoBateria;
    private MaterialCardView cardEstadoQuimico;
    private ImageView imgEstadoBateria;
    private ImageView imgEstadoQuimico;
    private TextView estadoRobot;
    private TextView textEstadoFumigacion;
    private TextView infoBateria;
    private TextView textBateria;
    private ImageView imagenBateria;
    private TextView infoNivelQuimico;
    private TextView textNivelQuimico;
    private TextView quimicoRobot;
    private ImageView imagenQuimico;
    private String mensajeInfoBateria;
    private String mensajeInfoNivelQuimico;
    private FloatingActionButton fab;
    private Chronometer cronometro;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;
    private Robot robot;
    private Fumigacion fumigacion;
    private Button detenerFumigacion;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private Button btnMas;

    private final int BATERIA_NIVEL_ALTO = 40;
    private final int BATERIA_NIVEL_MODERADO = 15;
    private final int BATERIA_NIVEL_BAJO = 5;
    private final int BATERIA_PROBLEMATICA = -1;
    private final int QUIMICO_NIVEL_ALTO = 40;
    private final int QUIMICO_NIVEL_MODERADO = 20;
    private final int QUIMICO_PROBLEMATICO = -1;
    private final int ROBOT_APAGADO = 1;
    private final int ROBOT_ENCENDIDO = 2;
    private final int ROBOT_OCUPADO = 3;
    private final int SEGUNDOS_MILIS = 1000;
    private final int MINUTOS_MILIS = SEGUNDOS_MILIS * 60;
    private final int HORAS_MILIS = MINUTOS_MILIS * 60;
    private final int DIAS_MILIS = HORAS_MILIS * 24;

    private boolean estadoFumigandoAnterior = false;


    public InicioFragment() {
        // Required empty public constructor
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Transiciones en los cambios de fragmento
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        firebaseFunctions = MyFirebase.getFunctionsInstance();
        //Instancia de la BD en Firebase
        firebaseDatabase = MyFirebase.getDatabaseInstance();
        //referencia de los robots
        referenceRobot = firebaseDatabase.getReference("robots");
        //Para que se mantenga sincronizado offline
        referenceRobot.keepSynced(true);
        referenceRobot.addValueEventListener(robotValueEventListener);

        //referencia de las fumigaciones para empezar a guardarlas
        referenceFumigacion = firebaseDatabase
            .getReference("fumigaciones_historial/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceFumigacion.keepSynced(true);


        /*refProgramadas = firebaseDatabase.getReference("fumigaciones_programadas/" + robot.getRobotId());
        refProgramadas.keepSynced(true);
        refProgramadas.addValueEventListener(fumigacionesEventListener);*/


        //Register para tomar los datos del fragmento de la nueva fumigación
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK && result.getData() != null){
                            //El callback se ejecuta cuando se cierre la activity de Nueva Fumigación
                            Bundle bundle = result.getData().getExtras();
                            fumigacion = (Fumigacion)bundle.getSerializable("fumigacion_nueva");
                            iniciarFumigacion(fumigacion);
                        }
                    }
                });

        Instance = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        cardEstadoBateria = vista.findViewById(R.id.cardEstadoBateria);
        cardEstadoQuimico = vista.findViewById(R.id.cardEstadoQuimico);
        imgEstadoBateria = vista.findViewById(R.id.imgEstadoBateria);
        imgEstadoQuimico = vista.findViewById(R.id.imgEstadoQuimico);

        estadoRobot = vista.findViewById(R.id.estadoRobot);
        textEstadoFumigacion = vista.findViewById(R.id.textEstadoFumigacion);
        infoBateria = vista.findViewById(R.id.infoBateria);
        textBateria = vista.findViewById(R.id.porcentajeBateria);
        imagenBateria = vista.findViewById(R.id.imagenBateria);
        infoNivelQuimico = vista.findViewById(R.id.infoNivelQuimico);
        textNivelQuimico = vista.findViewById(R.id.porcentajeQuimico);
        imagenQuimico = vista.findViewById(R.id.imagenQuimico);
        quimicoRobot = vista.findViewById(R.id.quimicoRobot);

        fab = vista.findViewById(R.id.fabFumigacion);
        fab.setOnClickListener(fabNuevaFumigacionListener);

        cronometro = vista.findViewById(R.id.Cronometro);

        detenerFumigacion = vista.findViewById(R.id.detenerFumigacion);
        detenerFumigacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detenerFumigacionAlertDialog();
            }
        });

        btnMas = vista.findViewById(R.id.btnMas);
        btnMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ConfigurarRobotActivity.class).putExtra("robot", robot));
                //ver si hace falta actualizar cosas en el onResume de este fragmento
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //determinarEstadoRobot(robot);
    }

    public void detenerFumigacionAlertDialog() {
        builder = new MaterialAlertDialogBuilder(getContext());

        builder.setMessage("¿Seguro querés detener la fumigación?");

        builder.setPositiveButton("detener", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                detenerFumigacion();
            }
        });

        builder.setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    public View.OnClickListener fabNuevaFumigacionListener = v -> nuevaFumigacion();

    public void nuevaFumigacion(){
        //startActivity(new Intent(getContext(), NuevaFumigacionActivity.class).putExtra("robot", robot));
        activityResultLauncher.launch(new Intent(getContext(), NuevaFumigacionActivity.class)
                .putExtra("robot_quimicos", robot.getQuimicosDisponibles())
                .putExtra("robot", robot));
    }

    public void determinarEstadoRobot(Robot robot){
        String porcentajeBateria;
        String porcentajeNivelQuimico;
        boolean statusBateria = false;
        boolean statusNivelQuimico = false;
        int fumigando = View.INVISIBLE;
        int bateria = robot.getBateria();
        int nivelQuimico = robot.getNivelQuimico();

        if(robot.isEncendido()) {
            statusBateria = verificarBateria(bateria);
            statusNivelQuimico = verificarNivelQuimico(nivelQuimico);


            if(bateria == -1)
                porcentajeBateria = "";
            else
                porcentajeBateria = bateria + "%";

            if(nivelQuimico == -1)
                porcentajeNivelQuimico = "";
            else
                porcentajeNivelQuimico = nivelQuimico + "%";


            if(robot.isFumigando()) {
                fumigando = View.VISIBLE;
                definirEstadoRobot(ROBOT_OCUPADO, "Fumigando...");
                //OPCION 1: ver el cronometro
            }
            else {
                fumigando = View.INVISIBLE;
                definirEstadoRobot(ROBOT_ENCENDIDO, "Encendido");
            }
        }
        else {
            porcentajeBateria = porcentajeNivelQuimico = "";
            definirEstadoRobot(ROBOT_APAGADO, "Apagado");
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
        }

        infoBateria.setText(mensajeInfoBateria);
        textBateria.setText(porcentajeBateria);
        infoNivelQuimico.setText(mensajeInfoNivelQuimico);
        textNivelQuimico.setText(porcentajeNivelQuimico);
        quimicoRobot.setText(robot.getUltimoQuimico());
        textEstadoFumigacion.setVisibility(fumigando);
        detenerFumigacion.setVisibility(fumigando);
        cronometro.setVisibility(fumigando);
    }

    /*private void habilitarFumigacion(boolean valor){
        //fab.setEnabled(valor);
    }*/

    private void definirEstadoRobot(int estado, String desc){
        estadoRobot.setText(desc);

        switch (estado){
            case ROBOT_ENCENDIDO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devEncendido));
                //habilitarFumigacion(true);
                return;

            case ROBOT_OCUPADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devOcupado));
                //habilitarFumigacion(false);
                return;

            case ROBOT_APAGADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devApagado));
                //habilitarFumigacion(false);
                return;
        }
    }

    @SuppressLint("NewApi")
    private boolean verificarBateria(int bateria) {
        if(bateria >= BATERIA_NIVEL_ALTO) {
            mensajeInfoBateria = "";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            cardEstadoBateria.setVisibility(View.GONE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoBateria = "Batería moderada: será necesario recargar pronto.";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            imgEstadoBateria.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoSugerencia);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_BAJO) {
            //Advertencia
            mensajeInfoBateria = "Batería baja: se recomienda recargar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_alert_24);
            imgEstadoBateria.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoAdvertencia);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);
            cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria == BATERIA_PROBLEMATICA) {
            mensajeInfoBateria = "Se recomienda reemplazar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            imgEstadoBateria.setImageResource(R.drawable.round_error_outline_24);
            int color = getResources().getColor(R.color.fondoAvisoBateria);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);
            cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        //Alerta
        mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
        imagenBateria.setImageResource(R.drawable.battery_alert_24);
        imgEstadoBateria.setImageResource(R.drawable.round_error_outline_24);
        int color = getResources().getColor(R.color.fondoAlerta);
        imgEstadoBateria.getDrawable().setTint(color);
        infoBateria.setTextColor(color);
        cardEstadoBateria.setVisibility(View.VISIBLE);
        return false;
    }

    @SuppressLint("NewApi")
    private boolean verificarNivelQuimico(int nivelQuimico) {
        if(nivelQuimico >= QUIMICO_NIVEL_ALTO) {
            mensajeInfoNivelQuimico = "";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            cardEstadoQuimico.setVisibility(View.GONE);
            return true;
        }
        if(nivelQuimico >= QUIMICO_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoNivelQuimico = "Nivel de químico moderado: será necesario recargar pronto.";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            imgEstadoQuimico.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoSugerencia);
            imgEstadoQuimico.getDrawable().setTint(color);
            infoNivelQuimico.setTextColor(color);
            cardEstadoQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        if(nivelQuimico == QUIMICO_PROBLEMATICO) {
            mensajeInfoNivelQuimico = "Se recomienda verificar el depósito del químico.";
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
            imgEstadoQuimico.setImageResource(R.drawable.round_error_outline_24);
            int color = getResources().getColor(R.color.fondoAvisoBateria);
            imgEstadoQuimico.getDrawable().setTint(color);
            infoNivelQuimico.setTextColor(color);
            cardEstadoQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        // Nivel de químico bajo
        mensajeInfoNivelQuimico = "Nivel de químico bajo: es necesario recargar.";
        imagenQuimico.setImageResource(R.drawable.ic_science_alert);
        imgEstadoQuimico.setImageResource(R.drawable.round_error_outline_24);
        int color = getResources().getColor(R.color.fondoAlerta);
        imgEstadoQuimico.getDrawable().setTint(color);
        infoNivelQuimico.setTextColor(color);
        cardEstadoQuimico.setVisibility(View.VISIBLE);
        return false;
    }


    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            Log.i("test", "estado fumigando inicio: " + estadoFumigandoAnterior);

            robot = dataSnapshot.child(Integer.toString(robot.getRobotId())).getValue(Robot.class);
            String tsInicio = (String)robot.getFumigacionActual().get("timestampInicio");

            if(estadoFumigandoAnterior && robot.isFumigando()){
                //toma el timestamp correcto
                iniciarCronometro(tsInicio);
                return;
            }
            if(estadoFumigandoAnterior == false && robot.isFumigando()){
                //setear el cronometro
                Log.i("test", "robot listener - arranca a fumigar");
                Log.i("test", "robot listener - ts inicio: " + tsInicio);
                iniciarCronometro(tsInicio);
            } else if(estadoFumigandoAnterior && robot.isFumigando() == false){
                //detener el cronometro
                detenerCronometro();
                Log.i("test", "robot listener - deja de fumigar");
            }
            estadoFumigandoAnterior = robot.isFumigando();

            Log.i("test", "estado fumigando fin: " + estadoFumigandoAnterior);
            determinarEstadoRobot(robot);
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void iniciarFumigacion(Fumigacion fumigacion){
        /*//Horrible pero para que ande
        this.fumigacion = fumigacion;
        robot.setFumigando(true);
        robot.convertirCantidadQuimicoPorArea(this.fumigacion.getCantidadQuimicoPorArea());
        cronometro.setBase(SystemClock.elapsedRealtime());
        //Actualizamos la hora a ahora
        this.fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis()));
        cronometro.start();
        updateRobot(robot);*/


        //UPDATE: apenas se crea la instantánea, crearla en FB en el nodo "fumigacionActual"
        //Horrible pero para que ande
        this.fumigacion = fumigacion;
        robot.setFumigando(true);
        //robot.convertirCantidadQuimicoPorArea(this.fumigacion.getCantidadQuimicoPorArea());
        this.fumigacion.setNivelBateriaInicial(robot.getBateria());
        this.fumigacion.setNivelQuimicoInicial(robot.getNivelQuimico());
        //Actualizamos la hora a ahora
        this.fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis()));
        //aca deberia crear en firebase
        referenceRobot.child(Integer.toString(robot.getRobotId())).child("fumigacionActual").setValue(this.fumigacion.toMap());

        //Este cronómetro acá está bien porque es una instantánea, debe arrancar ahora
        /*cronometro.setBase(SystemClock.elapsedRealtime());
        cronometro.start();*/
        updateRobot(robot);
    }

    public void iniciarCronometro(String tsInicio) {
        long diferencia = System.currentTimeMillis() - Long.parseLong(tsInicio);
        diferencia = diferencia < 0 ? 0 : diferencia;
        cronometro.setBase(SystemClock.elapsedRealtime() - diferencia);
        cronometro.start();
    }

    private void detenerCronometro(){
        cronometro.stop();
    }

    public void detenerFumigacion(){
       /* cronometro.stop();
        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));
        //Long tiempoTranscurrido = SystemClock.elapsedRealtime() - cronometro.getBase();
        //fumigacion.setTimestampFin(Long.toString(tiempoTranscurrido));
        robot.setFumigando(false);
        updateRobot(robot);
        updateFumigacion();//(fumigacion);*/

       //UPDATE: nodo fumigacionActual
        /*cronometro.stop();
        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));
        robot.setFumigando(false);
        updateRobot(robot);

        this.fumigacion.setNivelBateriaFinal(String.valueOf(robot.getBateria()));
        this.fumigacion.setNivelQuimicoFinal(String.valueOf(robot.getNivelQuimico()));

        updateFumigacion();//(fumigacion);*/

        //update con funcion de detener fumigacion
        HashMap<String, Object> params = new HashMap<>();
        params.put("robotId", robot.getRobotId());
        funcDetenerFumigacion(params).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                String resultado = "";

                if(task.isComplete()) {

                    if (task.isSuccessful()) {
                        resultado = task.getResult();
                        /*if (resultado.equalsIgnoreCase("Ok")) {
                            resultado = "Se inició la fumigación";
                        }*/
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Object details = ffe.getDetails();
                            Log.i("test", "Firebase Functions Exception: Code " + code);

                        }
                        resultado = e.getMessage();

                    }
                    //cambiar la forma en que informa al user que no lo hizo
                    //runOnUiThread(Toast.makeText(getApplicationContext(), resultado, Toast.LENGTH_SHORT)::show);
                    Log.i("test", "task no es successful, resultado: " + resultado);
                    detenerCronometro();
                    //fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));
                    robot.setFumigando(false);
                    updateRobot(robot);
                }
            }
        });
    }

    private Task<String> funcDetenerFumigacion(Map<String, Object> params){
        //invocamos a la Function
        return firebaseFunctions.getHttpsCallable("detenerFumigacion")
                .call(params)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        //esta continuacion se ejecuta en caso de éxito o falla, pero si la Task
                        //falla, entonces gtResult() va a arrojar una excepción la cual se va a propagar
                        String resultado = (String) task.getResult().getData();
                        Log.i("test", "Task detener fumigacion retorna resultado: " + resultado);
                        return resultado;
                    }
                });
    }

    public void updateRobot(Robot robot) {
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("fumigando", robot.isFumigando());
        //childUpdates.put("cantidadQuimicoPorArea", robot.getCantidadQuimicoPorArea());
        childUpdates.put("detencionAutomatica", false); //jaj
        referenceRobot.child(Integer.toString(robot.getRobotId())).updateChildren(childUpdates);
    }


    private ValueEventListener listenerFumigando = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };



    /*public void updateFumigacion(){//(Fumigacion fumigacion){
        //Necesita hacer una task porque primero consulta la cantidad y necesita esperar a que termine
        Task<DataSnapshot> task = referenceFumigacion.get();

        task.addOnCompleteListener(task1 -> {
            int cant;

            if (task1.isSuccessful()) {
                DataSnapshot snapshot = task1.getResult();
                cant = (int) snapshot.getChildrenCount();

                fumigacion.setFumigacionId("fh" + (cant + 1));
                referenceFumigacion.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMapHistorial());
            }
        });
    }*/
/*
    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            Long ahora = System.currentTimeMillis();

            // Buscamos las fumigaciones programadas en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                programada = item.getValue(Fumigacion.class);
                programada.setFumigacionId(item.getKey());

                Long fechaInicio = Long.parseLong(programada.getTimestampInicio());
                if(fechaInicio >= ahora){
                    if(!fumigacionesProgramadas.contains(programada)) {
                        fumigacionesProgramadas.add(programada);
                        //Log.i("TIMER | ADD LISTA", programada.getFumigacionId());
                        //programarEjecucionFumigaciones(programada);
                        //Alarma.empezarAlarma(getContext(), programada);
                    }
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };*/
}
