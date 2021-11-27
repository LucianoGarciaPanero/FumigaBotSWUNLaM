package com.example.fumigabot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ConfigurarRobotActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;

    private Robot robot;
    private TextView porcBateria;
    private TextView porcQuimico;
    private TextView txtInfoBateria;
    private TextView txtInfoQuimico;
    private Button btnCambiarQuimico;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;
    private String quimicoNuevo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_robot);

        robot = (Robot)getIntent().getSerializableExtra("robot");

        firebaseDatabase = MyFirebase.getDatabaseInstance();
        referenceRobot = firebaseDatabase.getReference("robots");
        referenceRobot.keepSynced(true);

        inicializarComponentes();
        cargarDatos();

        referenceRobot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                robot = dataSnapshot.child(String.valueOf(robot.getRobotId())).getValue(Robot.class);
                cargarDatos();

                return;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("WTF", "Failed to read value.", error.toException());
            }
        });

    }

    private void inicializarComponentes(){
        porcBateria = findViewById(R.id.porcBateria);
        porcQuimico = findViewById(R.id.porcQuimico);
        txtInfoBateria = findViewById(R.id.txtInfoBateriaConfig);
        txtInfoQuimico = findViewById(R.id.txtInfoQuimicoConfig);
        btnCambiarQuimico = findViewById(R.id.btnCambiarQuimico);
        btnCambiarQuimico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarQuimicoAlertDialog();
            }
        });
    }

    private void cambiarQuimicoAlertDialog(){
        builder = new MaterialAlertDialogBuilder(this);
        CharSequence[] array =  robot.getQuimicosDisponibles().toArray(new CharSequence[0]);
        int seleccionado = robot.getQuimicosDisponibles().indexOf(robot.getUltimoQuimico());
        builder.setSingleChoiceItems(array, seleccionado, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quimicoNuevo = array[which].toString();
                Log.i("test", "Seleccionado: " + quimicoNuevo);
            }
        });
        builder.setTitle("Cambiar químico").setPositiveButton("cambiar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cambiarQuimico();
            }
        });
        builder.setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void cambiarQuimico(){
        //Toast.makeText(this, "sel: " + quimicoNuevo, Toast.LENGTH_LONG).show();
        referenceRobot.child(String.valueOf(robot.getRobotId())).child("ultimoQuimico").setValue(quimicoNuevo);
    }



    private void cargarDatos(){
        if(robot == null)
            return;

        porcBateria.setText(robot.getBateria() + "%");
        porcQuimico.setText(robot.getNivelQuimico() + "%");
        txtInfoQuimico.setText("Actualmente el robot contiene " + robot.getUltimoQuimico());
    }
}
