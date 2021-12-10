package com.example.fumigabot;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;


public class VincularDispositivoActivity extends AppCompatActivity {

    private TextView txtPin;
    private Button btnVincular;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceUsers;
    private Robot robot;
    private String pin;

    private String userEmail;
    private String userName;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_dispositivo);

        // Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getDatabaseInstance();
        referenceRobot = firebaseDatabase.getReference("robots");
        referenceUsers = firebaseDatabase.getReference("users");

        userEmail = (String) getIntent().getExtras().get("userEmail");
        userName = (String) getIntent().getExtras().get("userName");

        configurarControles();
    }

    private void configurarControles() {
        txtPin = findViewById(R.id.txtPin);
        btnVincular = findViewById(R.id.btnVincular);

        btnVincular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pin = txtPin.getText().toString();

                if (!pin.equals("")) {

                    Task<DataSnapshot> task = referenceRobot.get();

                    task.addOnCompleteListener(task1 -> {
                        String mensajeVinculacion;

                        if (task1.isSuccessful()) {
                            DataSnapshot snapshot = task1.getResult();

                            if (snapshot.hasChild(pin)) {
                                guardarDatosDeSesion(userEmail, userName, pin);
                                insertarUsuarioFirebase(userEmail, pin);

                                mensajeVinculacion = "¡Robot vinculado!";

                                //Vamos al Home
                                robot = snapshot.child(pin).getValue(Robot.class);
                                Intent i = new Intent(getApplicationContext(), RobotHomeActivity.class);
                                i.putExtra("RobotVinculado", robot);
                                startActivity(i);
                                finish();
                            } else
                                mensajeVinculacion = "No se encontró el PIN ingresado";

                            runOnUiThread(Toast.makeText(getApplicationContext(), mensajeVinculacion, Toast.LENGTH_SHORT)::show);
                        }
                    });
                } else
                    Toast.makeText(getApplicationContext(), "Se debe ingresar un PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarDatosDeSesion(String userEmail, String userName, String pin) {
        SharedPreferences sp =
                getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();

        spEditor.putString("userEmail", userEmail);
        spEditor.putString("userName", userName);
        spEditor.putString("robotId", pin);
        spEditor.apply();
    }

    /*private void insertarUsuarioFirebase(String userEmail, String pin){
        getToken();



    }*/

    /**
     * Obtenemos el token de FCM para el smartphone vinculado,
     * luego, agregamos el usuario y su token a Firebase vinculados al robot indicado.
     */
    private void insertarUsuarioFirebase(String userEmail, String pin) {
        //Opcion 1: estructura simple
        String emailKey = userEmail.substring(0, userEmail.indexOf('@'));

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.i("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                token = task.getResult();
                /*Map<String, String> newUser = new HashMap<>();
                newUser.put("userEmail", userEmail);
                newUser.put("token", token);
                referenceUsers.child(pin).push().setValue(newUser);*/
                referenceUsers.child(pin).child(emailKey).setValue(token);
                // Log and toast
                Log.i("FCM", token);
                //return token;
            }
        });
    }
}
