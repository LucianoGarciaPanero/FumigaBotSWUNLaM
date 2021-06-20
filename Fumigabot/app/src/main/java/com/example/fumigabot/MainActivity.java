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
        reference.addValueEventListener(robotValueEventListener);

        textActividadRobot = findViewById(R.id.textActividadRobot);

        btnIniciarFumigacion = findViewById(R.id.btnIniciarFumigacion);
        btnIniciarFumigacion.setOnClickListener(btnIniciarFumigacionListener);
    }

    private View.OnClickListener btnIniciarFumigacionListener = new View.OnClickListener() {
        public void onClick(View v) {
            inicializarAlertDialog();
        }
    };

    public void inicializarAlertDialog(){
        builder = new AlertDialog.Builder(this);

        String titleAlertDialog;
        if(!robot.isFumigando())
            titleAlertDialog = "iniciar una ";
        else
            titleAlertDialog = "finalizar la ";
        builder.setMessage("¿Seguro querés " + titleAlertDialog + "fumigación?");

        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!robot.isFumigando())
                    updateRobot(robot.getRobotId(), true);
                else
                    updateRobot(robot.getRobotId(), false);

                determinarEstadoRobot(robot.isFumigando());
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

    public void determinarEstadoRobot(boolean fumigando){
        if(fumigando){
            textActividadRobot.setText("FUMIGANDO...");
            btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
        }
        else {
            textActividadRobot.setText("ESPERANDO ÓRDENES...");
            btnIniciarFumigacion.setText("FUMIGAR");
        }
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // This method is called once with the initial value and again
            // whenever data at this location is updated.
            robot = dataSnapshot.child("0").getValue(Robot.class);
            determinarEstadoRobot(robot.isFumigando());
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void updateRobot(int robotId, boolean fumigando) {
        Robot robot = new Robot(robotId, fumigando);
        Map<String, Object> robotValues = robot.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(robotId + "/", robotValues);

        reference.updateChildren(childUpdates);
    }
}
