package com.example.fumigabot;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
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


public class InicialActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private String IDRobot = "";

    private ImageView anim_robot;
    //private AnimatedVectorDrawableCompat avd;
    //private AnimatedVectorDrawable avd2;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicial);

        anim_robot = findViewById(R.id.imageRobot);
        /*Drawable draw = anim_robot.getDrawable();
        if (draw instanceof AnimatedVectorDrawableCompat) {
            avd = (AnimatedVectorDrawableCompat) draw;
            avd.start();
        } else if (draw instanceof AnimatedVectorDrawable) {
            avd2 = (AnimatedVectorDrawable) draw;
            avd2.start();
        }*/

        //Vemos si tiene vinculado algo
        IDRobot = getIdRobotSP();


        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots");
        reference.addValueEventListener(robotValueEventListener);

        if(IDRobot.isEmpty())
            obtenerRobot();
    }

    private String getIdRobotSP() {
        //Leemos de Shared Preferences
        SharedPreferences preferences = this.getSharedPreferences("Fumigabot_Pin_Dev", Context.MODE_PRIVATE);
        String res = preferences.getString("IDRobot", "");

        return res;
    }

    private void obtenerRobot() {
        if(IDRobot != "") { //quiere decir que tiene algo, puedo ir a buscar a la base de datos
            //Pasamos al Home y vemos toda la data del robot vinculado
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.putExtra("RobotVinculado",robot);
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
            if(IDRobot.isEmpty())
                return;
            robot = dataSnapshot.child(IDRobot).getValue(Robot.class);
            obtenerRobot();
            IDRobot = "";
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };
}
