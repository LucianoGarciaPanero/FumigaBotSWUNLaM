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
import android.util.Log;
import android.widget.ImageView;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        anim_robot = findViewById(R.id.logoCompleto);

        // Vemos si existe una sesión iniciada
        userEmail = getDatosDeSesion().get("userEmail");

        if(userEmail == null) {
            goToSignInActivity();
        } else {
            // Hay una sesión iniciada. Vemos si hay un robot vinculado.
            robotId = getIdRobotSP();

            //Instancia y referencia de la BD en Firebase
            firebaseDatabase = MyFirebase.getDatabaseInstance();
            reference = firebaseDatabase.getReference("robots");
            reference.addValueEventListener(robotValueEventListener);

            if(robotId.isEmpty())
                obtenerRobot();
        }
    }

    private Map<String, String> getDatosDeSesion() {
        Map<String, String> datosDeSesion = new HashMap<>();

        SharedPreferences sp =
            getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);

        String userEmail = sp.getString("userEmail", null);
        String userName = sp.getString("userName", null);
        datosDeSesion.put("userEmail", userEmail);
        datosDeSesion.put("userName", userName);

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

    private String getIdRobotSP() {
        SharedPreferences sp =
            getSharedPreferences(String.valueOf(R.string.sp_robot_vinculado), Context.MODE_PRIVATE);
        String datos = sp.getString("robotId", "");

        return datos;
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            //Si detecta algo, lo trae
            if (robotId.isEmpty()) {
                return;
            }
            robot = dataSnapshot.child(robotId).getValue(Robot.class);
            obtenerRobot();
            robotId = "";
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    private void obtenerRobot() {
        if(robotId != "") { //quiere decir que tiene algo, puedo ir a buscar a la base de datos
            //Pasamos al Home y vemos toda la data del robot vinculado
            Intent i = new Intent(getApplicationContext(), RobotHomeActivity.class);
            i.putExtra("RobotVinculado", robot);
            startActivity(i);
        }
        else {
            //Quiere decir que tiene que vincular un nuevo dispositivo
            startActivity(new Intent(getApplicationContext(), VincularDispositivoActivity.class));
        }
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