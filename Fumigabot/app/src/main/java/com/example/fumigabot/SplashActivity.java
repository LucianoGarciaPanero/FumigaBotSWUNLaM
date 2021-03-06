package com.example.fumigabot;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;


public class SplashActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private String robotId = "";
    private String userEmail = "";
    private ImageView anim_robot;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_splash);

        crearCanalNotificaciones();
        //borrarDatosDeSesion();

        anim_robot = findViewById(R.id.logoCompleto);

        // Vemos si existe una sesión iniciada
        userEmail = getDatosDeSesion().get("userEmail");

        if(userEmail == null) {
            goToSignInActivity();
        } else {
            // Hay una sesión iniciada. Vemos si hay un robot vinculado.
            robotId = getDatosDeSesion().get("robotId");

            //Instancia y referencia de la BD en Firebase
            firebaseDatabase = MyFirebase.getDatabaseInstance();
            reference = firebaseDatabase.getReference("robots/" + robotId);
            getRobotVinculado();
        }
    }

    private Map<String, String> getDatosDeSesion() {
        Map<String, String> datosDeSesion = new HashMap<>();

        SharedPreferences sp =
            getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);

        String userEmail = sp.getString("userEmail", null);
        String userName = sp.getString("userName", null);
        String robotId = sp.getString("robotId", null);
        datosDeSesion.put("userEmail", userEmail);
        datosDeSesion.put("userName", userName);
        datosDeSesion.put("robotId", robotId);

        return datosDeSesion;
    }

    private void goToSignInActivity() {
        Intent i = new Intent(getApplicationContext(), SignInActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * Borra los datos de sesión (por ahora solo email) de la cuenta Gmail con la que
     * ingresamos anteriormente a la app.
     */
    private void borrarDatosDeSesion() {
        SharedPreferences sp =
            getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.clear();
        spEditor.apply();
    }

    private void getRobotVinculado(){
        Task<DataSnapshot> task = reference.get();

        task.addOnCompleteListener(task1 -> {
           if(task1.isSuccessful()){
               robot = task1.getResult().getValue(Robot.class);
               goToRobotHomeActivity();
           }
        });
    }

    private void goToRobotHomeActivity() {
        //Pasamos al Home y vemos toda la data del robot vinculado
        Intent i = new Intent(getApplicationContext(), RobotHomeActivity.class);
        i.putExtra("RobotVinculado", robot);
        startActivity(i);
        finish();
    }

    private void crearCanalNotificaciones(){
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("DEFY",
                    "DEFY",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}