package com.example.fumigabot;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class RobotHistorialActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
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

        //Obtiene el ID del robot, único dato necesario para conocer sus historiales
        robotId = getIntent().getExtras().getInt("robotId");

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("fumigaciones/" + robotId);
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);
        reference.addValueEventListener(fumigacionesEventListener);

        tablaHistorial = findViewById(R.id.tablaHistorial);
    }

    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // This method is called once with the initial value and again
            // whenever data at this location is updated.

            // Limpiamos todas las fumigaciones anteriores
            // ya que si se agrega o modifica una, va a cargar repetidas
            listaFumigaciones.clear();

            // Buscamos las fumigaciones en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacion = item.getValue(Fumigacion.class);
                fumigacion.setFumigacionId(item.getKey());
                listaFumigaciones.add(fumigacion);
            }

            // Ordena la lista ascendentemente según timestampInicio
            Collections.sort(listaFumigaciones);
            generarTablaFumigaciones(listaFumigaciones);
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void generarTablaFumigaciones(ArrayList<Fumigacion> listaFumigaciones){
        // Limpiamos todas las filas views anteriores
        // ya que si no, se van a agregar repetidas
        tablaHistorial.removeAllViews();

        agregarHeader();
        for(Fumigacion fumigacion : listaFumigaciones)
            agregarFila(fumigacion);
    }

    public void agregarHeader(){
        //Crea nueva fila
        TableRow filaTitulo = new TableRow(this);
        filaTitulo.setBackgroundColor(Color.BLACK);
        filaTitulo.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView labelHeader = new TextView(this);
        labelHeader.setText("HISTORIAL DE FUMIGACIONES");
        labelHeader.setTextColor(Color.WHITE);
        labelHeader.setPadding(1, 1, 1, 1);
        filaTitulo.addView(labelHeader);

        tablaHistorial.addView(filaTitulo, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }

    public void agregarFila(Fumigacion fumigacion){

        //Crea nueva fila
        TableRow nuevaFila = new TableRow(this);
        nuevaFila.setBackgroundColor(Color.GRAY);
        nuevaFila.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        // Formatea los timestamps a mostrar
        SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date fechaHoraInicio = new Date(Long.parseLong(fumigacion.getTimestampInicio()));
        Date fechaHoraFin = new Date(Long.parseLong(fumigacion.getTimestampFin()));
        String fechaHoraInicioFormateada = formateador.format(fechaHoraInicio);
        String fechaHoraFinFormateada = formateador.format(fechaHoraFin);

        //Columna 1
        TextView labelTimestampInicio = new TextView(this);
        labelTimestampInicio.setText(fechaHoraInicioFormateada);
        labelTimestampInicio.setTextColor(Color.WHITE);
        labelTimestampInicio.setPadding(1, 1, 1, 1);
        nuevaFila.addView(labelTimestampInicio);

        //Columna 2
        TextView labelTimestampFin = new TextView(this);
        labelTimestampFin.setText(fechaHoraFinFormateada);
        labelTimestampFin.setTextColor(Color.WHITE);
        labelTimestampFin.setPadding(1, 1, 1, 1);
        nuevaFila.addView(labelTimestampFin);

        //Agrega fila
        tablaHistorial.addView(nuevaFila, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }
}
