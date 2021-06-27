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



public class VincularDispositivoActivity extends AppCompatActivity {

    private TextView txtPin;
    private Button btnVincular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vincular_dispositivo);


        configurarControles();
    }

    private void configurarControles() {

        txtPin = findViewById(R.id.txtPin);
        btnVincular = findViewById(R.id.btnVincular);


        btnVincular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(txtPin.getText().toString() != "")
                {
                    //Vincular dispositivo: el PIN es el ID del robot
                    guardarPinSP(txtPin.getText().toString());
                    //Actualizamos en Firebase la vinculación
                    //updateRobot(robot);

                    //Vamos al Home
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    i.putExtra("PinRobot", Integer.parseInt(txtPin.getText().toString()));

                    startActivity(i);
                    finish();
                }

                //Toast.makeText(this, "Se debe ingresar un PIN", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarPinSP(String pin)
    {
        //Guardamos en SharedPreferences el PIN
        SharedPreferences sp = getApplicationContext().getSharedPreferences("Fumigabot_Pin_Dev", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("IDRobot", pin);
        editor.apply();
    }

}
