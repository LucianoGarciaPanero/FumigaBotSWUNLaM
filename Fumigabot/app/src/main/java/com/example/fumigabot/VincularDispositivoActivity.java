package com.example.fumigabot;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class VincularDispositivoActivity extends AppCompatActivity {

    private TextView txtPin;
    private Button btnVincular;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private String pin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_dispositivo);


        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots");


        configurarControles();
    }

    private void configurarControles() {

        txtPin = findViewById(R.id.txtPin);
        btnVincular = findViewById(R.id.btnVincular);


        btnVincular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pin = txtPin.getText().toString();
                String mensajeVinculacion = "No se encontró el PIN ingresado";

                if(!pin.equals("")) {

                    Task<DataSnapshot> task = reference.get();

                    task.addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DataSnapshot snapshot = task1.getResult();
                            if (snapshot.hasChild(pin)) {
                                robot = snapshot.child(pin).getValue(Robot.class);

                                guardarPinSP(pin);

                                //Vamos al Home
                                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                                i.putExtra("RobotVinculado", robot);

                                startActivity(i);
                                finish();
                            } else {
                                robot = null;
                            }
                        }
                    });
                }
                else
                    mensajeVinculacion = "Se debe ingresar un PIN";

                Toast.makeText(getApplicationContext(), mensajeVinculacion, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarPinSP(String pin) {
        //Guardamos en SharedPreferences el PIN
        SharedPreferences sp = getApplicationContext().getSharedPreferences("Fumigabot_Pin_Dev", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("robotId", pin);
        editor.apply();
    }

}
