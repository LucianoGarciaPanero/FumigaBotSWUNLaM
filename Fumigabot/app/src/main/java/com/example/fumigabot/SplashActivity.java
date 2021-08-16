package com.example.fumigabot;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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


public class SplashActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private String robotId = "";
    private ImageView anim_robot;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        anim_robot = findViewById(R.id.imageRobot);

        //Vemos si tiene vinculado algo
        robotId = getIdRobotSP();

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots");
        reference.addValueEventListener(robotValueEventListener);

        if(robotId.isEmpty())
            obtenerRobot();
    }

    private String getIdRobotSP() {
        //Leemos de Shared Preferences
        SharedPreferences preferences = this.getSharedPreferences("Fumigabot_Pin_Dev", Context.MODE_PRIVATE);
        String res = preferences.getString("robotId", "");

        return res;
    }

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

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            //Si detecta algo, lo trae
            if(robotId.isEmpty())
                return;
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
}