package com.example.fumigabot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;


public class RobotHomeActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceFumigacion;
    private TextView textActividadRobot;
    private TextView textBateria;
    private TextView infoBateria;
    private Button btnIniciarFumigacion;
    private Button btnVerHistorialFumigaciones;
    private Robot robot;
    private Fumigacion fumigacion;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    private String mensajeInfoBateria;
    private Chronometer cronometro;

    private final int BATERIA_NIVEL_ALTO = 40;
    private final int BATERIA_NIVEL_MODERADO = 15;
    private final int BATERIA_NIVEL_BAJO = 5;
    private final int BATERIA_PROBLEMATICA = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_home);

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        referenceRobot = firebaseDatabase.getReference("robots");
        //Para que se mantenga sincronizado offline
        referenceRobot.keepSynced(true);
        referenceRobot.addValueEventListener(robotValueEventListener);

        robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");

        //referencia de las fumigaciones para empezar a guardarlas
        referenceFumigacion = firebaseDatabase.getReference("fumigaciones/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceFumigacion.keepSynced(true);

        textActividadRobot = findViewById(R.id.textActividadRobot);
        textBateria = findViewById(R.id.textBateria);
        infoBateria = findViewById(R.id.infoBateria);
        btnIniciarFumigacion = findViewById(R.id.btnIniciarFumigacion);
        btnIniciarFumigacion.setOnClickListener(btnIniciarFumigacionListener);
        btnVerHistorialFumigaciones = findViewById(R.id.btnVerHistorialFumigaciones);
        btnVerHistorialFumigaciones.setOnClickListener(btnVerHistorialFumigacionesListener);
        cronometro = findViewById(R.id.Cronometro);
    }

    private View.OnClickListener btnIniciarFumigacionListener = v -> inicializarAlertDialog();

    private View.OnClickListener btnVerHistorialFumigacionesListener = v -> {
        Intent i = new Intent(getApplicationContext(), RobotHistorialActivity.class);
        i.putExtra("robotId", robot.getRobotId());
        startActivity(i);
    };

    public void inicializarAlertDialog(){
        builder = new AlertDialog.Builder(this, R.style.alertDialogStyle);

        String titleAlertDialog;
        if(!robot.isFumigando())
            titleAlertDialog = "iniciar una ";
        else
            titleAlertDialog = "finalizar la ";
        builder.setMessage("¿Seguro querés " + titleAlertDialog + "fumigación?");

        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!robot.isEncendido()) {
                    builder.setMessage("El dispositivo se encuentra apagado");
                    return;
                }
                else {
                    if (!robot.isFumigando()) {
                        robot.setFumigando(true);
                        iniciarFumigacion();
                    } else {
                        robot.setFumigando(false);
                        detenerFumigacion();
                    }

                    updateRobot(robot);
                }
                determinarEstadoRobot(robot);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    public void determinarEstadoRobot(Robot robot){
        String estado;
        String porcentajeBateria;
        boolean status = false;
        int fumigando = View.INVISIBLE;
        int bateria = robot.getBateria();

        if(robot.isEncendido()){
            status = verificarBateria(bateria);

            if(bateria == -1)
                porcentajeBateria = "Batería: con problemas"; //\n\n\nENCENDIDO\n\n";
            else
                porcentajeBateria = "Batería: " + bateria + "%"; //\n\n\nENCENDIDO\n\n";

            if(robot.isFumigando()) {
                estado = "FUMIGANDO...";
                btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
                fumigando = View.VISIBLE;
            }
            else {
                estado = "ESPERANDO ÓRDENES...";
                btnIniciarFumigacion.setText("FUMIGAR");
                fumigando = View.INVISIBLE;
            }
        }
        else {
            porcentajeBateria = "APAGADO\n\n\n";
            mensajeInfoBateria = "";
            infoBateria.setBackgroundResource(R.color.activityBackground);
            estado = "Encender el dispositivo para comenzar";
            btnIniciarFumigacion.setText("FUMIGAR");
        }

        infoBateria.setText(mensajeInfoBateria);
        textBateria.setText(porcentajeBateria);
        textActividadRobot.setText(estado);
        btnIniciarFumigacion.setEnabled(status);
        cronometro.setVisibility(fumigando);
    }

    private boolean verificarBateria(int bateria) {
        if(bateria >= BATERIA_NIVEL_ALTO) {
            mensajeInfoBateria = "";
            infoBateria.setBackgroundResource(R.color.activityBackground);
            return true;
        }
        else if(bateria >= BATERIA_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoBateria = "Batería moderada: será necesario recargar pronto.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_sugerencia);
            return true;
        }
        else if(bateria >= BATERIA_NIVEL_BAJO) {
            //Advertencia
            mensajeInfoBateria = "Batería baja: se recomienda recargar la batería.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_advertencia);
            return true;
        }
        else if(bateria == BATERIA_PROBLEMATICA) {
            mensajeInfoBateria = "Advertencia: se recomienda reemplazar la batería.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_problemas);
            return true;
        }
        else
        {
            //Alerta
            mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_alerta);
            return false;
        }
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            robot = dataSnapshot.child(robot.getRobotId() + "").getValue(Robot.class);
            determinarEstadoRobot(robot);
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void iniciarFumigacion(){
        fumigacion = new Fumigacion();
        cronometro.setBase(SystemClock.elapsedRealtime());
        //cronometro.setBase(System.currentTimeMillis());//Clock.elapsedRealtime());
        fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis())); //Long.toString(cronometro.getBase()));
        cronometro.start();
    }

    public void detenerFumigacion(){
        cronometro.stop();
        //long tiempoTranscurrido = SystemClock.elapsedRealtime() - cronometro.getBase();
        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));//(Long.toString(tiempoTranscurrido));

        updateFumigacion(fumigacion);
    }

    public void updateRobot(Robot robot) {
        referenceRobot.child(Integer.toString(robot.getRobotId()))
                .child("fumigando")
                .setValue(robot.isFumigando());
    }

    public void updateFumigacion(Fumigacion fumigacion){
        //Necesita hacer una task porque primero consulta la cantidad y necesita esperar a que termine
        Task<DataSnapshot> task = referenceFumigacion.get();

        task.addOnCompleteListener(task1 -> {
            int cant;

            if (task1.isSuccessful()) {
                DataSnapshot snapshot = task1.getResult();
                cant = (int) snapshot.getChildrenCount();

                fumigacion.setFumigacionId("f" + (cant + 1));
                referenceFumigacion.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMap());
            }
        });
    }
}
