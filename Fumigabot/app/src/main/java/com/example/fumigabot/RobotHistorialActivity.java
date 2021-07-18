package com.example.fumigabot;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

            // Ordena la lista descendentemente según timestampInicio
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
        // Limpiamos todas las filas de fumigaciones anteriores
        // ya que si no, se van a agregar repetidas
        // si hay más de dos childs es porque hay fumigaciones cargadas
        if(tablaHistorial.getChildCount() > 2)
            tablaHistorial.removeViews(2, listaFumigaciones.size());

        for(Fumigacion fumigacion : listaFumigaciones)
            agregarFila(fumigacion);
    }

    public void agregarFila(Fumigacion fumigacion){

        //Crea nueva fila
        TableRow nuevaFila = new TableRow(this);
        nuevaFila.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));//(Color.parseColor("#2F3C7E"));
        nuevaFila.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        // Columna Id
        TextView labelFumigacionId = new TextView(this);
        labelFumigacionId.setText("#" + fumigacion.getFumigacionId().charAt(1));
        labelFumigacionId.setTextColor(getResources().getColor(R.color.colorPrimary));//(Color.parseColor("#FBEAEB"));
        labelFumigacionId.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        nuevaFila.addView(labelFumigacionId);

        // Formatea los timestamps a mostrar
        SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date fechaHoraInicio = new Date(Long.parseLong(fumigacion.getTimestampInicio()));
        Date fechaHoraFin = new Date(Long.parseLong(fumigacion.getTimestampFin()));
        String fechaHoraInicioFormateada = formateador.format(fechaHoraInicio);
        String fechaHoraFinFormateada = formateador.format(fechaHoraFin);

        // Columna fechaHoraInicio
        TextView labelTimestampInicio = new TextView(this);
        labelTimestampInicio.setText(fechaHoraInicioFormateada);
        labelTimestampInicio.setTextColor(getResources().getColor(R.color.colorPrimary));//(Color.parseColor("#FBEAEB"));
        labelTimestampInicio.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        nuevaFila.addView(labelTimestampInicio);

        // Columna fechaHoraFin
        TextView labelTimestampFin = new TextView(this);
        labelTimestampFin.setText(fechaHoraFinFormateada);
        labelTimestampFin.setTextColor(getResources().getColor(R.color.colorPrimary));//(Color.parseColor("#FBEAEB"));
        labelTimestampFin.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        nuevaFila.addView(labelTimestampFin);

        // Calcula duración de la fumigación en minutos y segundos
        /*long diferenciaTiempos = fechaHoraFin.getTime() - fechaHoraInicio.getTime();
        int segundos = (int) diferenciaTiempos / 1000;
        int minutos = (segundos % 3600) / 60;
        segundos = (segundos % 3600) % 60;*/

        // Columna Duración
        TextView labelDuracion = new TextView(this);
        //labelDuracion.setText(minutos + "m " + segundos + "s");
        labelDuracion.setText(calcularDuracion(fechaHoraFin.getTime() - fechaHoraInicio.getTime()));
        labelDuracion.setTextColor(getResources().getColor(R.color.colorPrimary));//(Color.parseColor("#FBEAEB"));
        labelDuracion.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        nuevaFila.addView(labelDuracion);


        //nuevaFila.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        //Agrega fila
        tablaHistorial.addView(nuevaFila, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }

    public String calcularDuracion(long diferencia) {
        long segundosMilis = 1000; //1000 ms = 1seg
        long minutosMilis = segundosMilis * 60; //1m = 60seg
        long horasMilis = minutosMilis * 60; //1h = 60m
        //no creo que vaya a durar días, pero para probar:
        long diasMilis = horasMilis * 24; //1d = 24h

        //Ahora, calculamos el paso del tiempo real
        long dias = diferencia / diasMilis;
        diferencia %= diasMilis;

        long horas = diferencia / horasMilis;
        diferencia %= horasMilis;

        long minutos = diferencia / minutosMilis;
        diferencia &= minutosMilis;

        long segundos = diferencia / segundosMilis;


        String resultado = "";

        if(dias >= 1){
            resultado = dias + "d ";
        }
        else if(horas >= 1){
            resultado += horas + "h ";
        }

        resultado += minutos + "m " + segundos + "s";

        return resultado;
    }
}
