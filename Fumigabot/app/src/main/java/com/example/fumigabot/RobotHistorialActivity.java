package com.example.fumigabot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RobotHistorialActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private int robotId;
    private ArrayList<Fumigacion> listaFumigaciones = new ArrayList<>();
    private Fumigacion fumigacion;
    private TableLayout tablaHistorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_historial);

        robotId = getIntent().getExtras().getInt("robotId");

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("fumigaciones/" + robotId);
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);
        reference.addValueEventListener(robotFumigacionesEventListener);

        tablaHistorial = findViewById(R.id.tablaHistorial);
    }

    private ValueEventListener robotFumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // This method is called once with the initial value and again
            // whenever data at this location is updated.

            //Buscamos las fumigaciones en Firebase
            listaFumigaciones.clear(); //Limpiamos todas las anteriores
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacion = item.getValue(Fumigacion.class);
                listaFumigaciones.add(fumigacion);
            }
            agregarFilas(listaFumigaciones);

        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void agregarFilas(ArrayList<Fumigacion> listaFumigaciones){
        tablaHistorial.removeAllViews(); //Limpiamos todas las filas anteriores

        for(Fumigacion fumigacion : listaFumigaciones) {
            agregarUnaFila(fumigacion, listaFumigaciones.indexOf(fumigacion));
        }
    }

    public void agregarUnaFila(Fumigacion fumigacion, int idFumigacion){

        //Crea nueva fila
        TableRow nuevaFila = new TableRow(this);
        nuevaFila.setId(idFumigacion); //Inchequeable esto del id
        nuevaFila.setBackgroundColor(Color.GRAY);
        nuevaFila.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        //Columna 1
        TextView labelTimestampInicio = new TextView(this);
        labelTimestampInicio.setId(idFumigacion + 1); //Inchequeable esto del id
        labelTimestampInicio.setText(Long.toString(fumigacion.getTimestampInicio()));
        labelTimestampInicio.setTextColor(Color.WHITE);
        labelTimestampInicio.setPadding(5, 5, 5, 5);
        nuevaFila.addView(labelTimestampInicio);

        //Columna 2
        TextView labelTimestampFin = new TextView(this);
        labelTimestampFin.setId(idFumigacion + 2); //Inchequeable esto del id
        labelTimestampFin.setText(Long.toString(fumigacion.getTimestampFin()));
        labelTimestampFin.setTextColor(Color.WHITE);
        labelTimestampFin.setPadding(5, 5, 5, 5);
        nuevaFila.addView(labelTimestampFin);

        //Agrega fila
        tablaHistorial.addView(nuevaFila, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }
}
