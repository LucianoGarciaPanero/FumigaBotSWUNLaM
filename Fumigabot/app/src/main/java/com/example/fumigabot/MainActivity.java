package com.example.fumigabot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private TextView textActividadRobot;
    private TextView textBateria;
    private TextView infoBateria;
    private Button btnIniciarFumigacion;
    private Robot robot;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    private String mensajeInfoBateria;
    private final int BATERIA_NIVEL_ALTO = 40;
    private final int BATERIA_NIVEL_MODERADO = 15;
    private final int BATERIA_NIVEL_BAJO = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots");
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);
        reference.addValueEventListener(robotValueEventListener);

        robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");

        textActividadRobot = findViewById(R.id.textActividadRobot);
        textBateria = findViewById(R.id.textBateria);
        infoBateria = findViewById(R.id.infoBateria);
        btnIniciarFumigacion = findViewById(R.id.btnIniciarFumigacion);
        btnIniciarFumigacion.setOnClickListener(btnIniciarFumigacionListener);
    }

    private View.OnClickListener btnIniciarFumigacionListener = new View.OnClickListener() {
        public void onClick(View v) {
            inicializarAlertDialog();
        }
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
                        //updateRobot(robot.getRobotId(), true);
                    } else
                        robot.setFumigando(false);

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


        if(robot.isEncendido()){
            status = verificarBateria(robot.getBateria());
            porcentajeBateria = "Batería: " + robot.getBateria() + "%"; //\n\n\nENCENDIDO\n\n";
            if(robot.isFumigando()) {
                estado = "FUMIGANDO...";
                btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
            }
            else {
                estado = "ESPERANDO ÓRDENES...";
                btnIniciarFumigacion.setText("FUMIGAR");
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
        else {// if(bateria <5) {
            //Alerta
            mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_alerta);
            return false;
        }
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // This method is called once with the initial value and again
            // whenever data at this location is updated.

            robot = dataSnapshot.child(robot.getRobotId()+"").getValue(Robot.class);
            determinarEstadoRobot(robot);
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void updateRobot(Robot robot) {
        /*Map<String, Object> robotValues = robot.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(robot.getRobotId() + "/", robotValues);

        reference.updateChildren(childUpdates);*/

        //Desde la app solamente deberíamos poder modificar si está fumigando o no
        //El día de mañana podemos mandarle la orden de apagar si queremos
        reference.child(robot.getRobotId()+"").child("fumigando").setValue(robot.isFumigando());
    }
}
