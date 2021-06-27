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
    private Button btnIniciarFumigacion;
    private Robot robot;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

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
        int idRobot = getIntent().getIntExtra("PinRobot", -1);

        if(robot == null && idRobot == -1) {
            Toast.makeText(getApplicationContext(), "No se encontró el robot vinculado", Toast.LENGTH_LONG).show();
        }
        else if(idRobot != -1)
        {
            //Vino de vincular recien
            robot = new Robot();
            robot.setRobotId(idRobot);
        }

        textActividadRobot = findViewById(R.id.textActividadRobot);
        textBateria = findViewById(R.id.textBateria);
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
                if(!robot.isEncendido())
                {
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
        String bateria;
        boolean habilitado = robot.isEncendido();


        if(robot.isEncendido()){
            bateria = "Batería: " + robot.getBateria() + "%\n\n\nENCENDIDO\n\n";
            if(robot.isFumigando())
            {
                estado = "FUMIGANDO...";
                btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
            }
            else
            {
                estado = "ESPERANDO ÓRDENES...";
                btnIniciarFumigacion.setText("FUMIGAR");
            }
        }
        else
        {
            bateria = "APAGADO\n\n\n";
            estado = "Encender el dispositivo para comenzar";
            btnIniciarFumigacion.setText("FUMIGAR");
        }

        textBateria.setText(bateria);
        textActividadRobot.setText(estado);
        btnIniciarFumigacion.setEnabled(habilitado);
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
        Map<String, Object> robotValues = robot.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(robot.getRobotId() + "/", robotValues);

        reference.updateChildren(childUpdates);
    }
}
