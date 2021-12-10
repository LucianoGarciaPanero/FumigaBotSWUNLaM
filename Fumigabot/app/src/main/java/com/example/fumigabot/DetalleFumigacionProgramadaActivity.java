package com.example.fumigabot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DetalleFumigacionProgramadaActivity extends AppCompatActivity {

    private Fumigacion programada;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceProgramadas;
    private TextView fechaProgramada;
    private TextView quimicoProgramada;
    private TextView cantidadProgramada;
    private TextView esRecurrente;
    private CalendarView calendarioProgramada;
    private TimePicker horaProgramada;
    private Button btnCancelar;
    private Button btnGuardar;
    private Date fechaNueva = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_fumigacion_programada);

        programada = (Fumigacion) getIntent().getSerializableExtra("fumigacionProgramadaSeleccionada");
        int robotId = getIntent().getIntExtra("idRobot", -1);
        if(robotId == -1)
            Log.i("DETALLE FP", "robot id no encontrado");

        firebaseDatabase = MyFirebase.getDatabaseInstance();
        referenceProgramadas = firebaseDatabase.getReference("fumigaciones_programadas/" + robotId);
        //Para que se mantenga sincronizado offline
        referenceProgramadas.keepSynced(true);

        inicializarControles();
        cargarDatos();
    }

    private void inicializarControles(){
        fechaProgramada = findViewById(R.id.fechaProgramada);
        quimicoProgramada = findViewById(R.id.quimicoProgramada);
        esRecurrente = findViewById(R.id.txtEsRecurrente);
        cantidadProgramada = findViewById(R.id.cantidadProgramada);
        calendarioProgramada = findViewById(R.id.calendarioProgramada);
        calendarioProgramada.setOnDateChangeListener(calendarioListener);
        horaProgramada = findViewById(R.id.horaProgramada);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(btnCancelarListener);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(btnGuardarListener);
    }

    private CalendarView.OnDateChangeListener calendarioListener = new CalendarView.OnDateChangeListener() {
        @Override
        public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
            fechaNueva = new Date(year  - 1900, month, dayOfMonth);
        }
    };

    private View.OnClickListener btnCancelarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener btnGuardarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            modificarFumigacionProgramada();
            Toast.makeText(getApplicationContext(), "Fumigación modificada", Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private void cargarDatos(){
        fechaProgramada.setText(programada.darFormatoFechaInicio());
        quimicoProgramada.setText(programada.getQuimicoUtilizado());
        cantidadProgramada.setText(programada.getCantidadQuimicoPorArea());

        String cantidad = programada.getCantidadQuimicoPorArea();
        if(cantidad.startsWith("A"))
            cantidad = "Alta";
        else if(cantidad.startsWith("B"))
            cantidad = "Baja";
        else
            cantidad = "Media";

        cantidadProgramada.setText(cantidad);

        calendarioProgramada.setDate(Long.parseLong(programada.getTimestampInicio()));

        Date horaInicio = new Date(Long.parseLong(programada.getTimestampInicio()));
        horaProgramada.setHour(horaInicio.getHours());
        horaProgramada.setMinute(horaInicio.getMinutes());

        if(programada.isRecurrente()){
            esRecurrente.setVisibility(View.VISIBLE);
        } else {
            esRecurrente.setVisibility(View.GONE);
        }
    }

    private void modificarFumigacionProgramada(){
        fechaNueva.setHours(horaProgramada.getHour());
        fechaNueva.setMinutes(horaProgramada.getMinute());
        fechaNueva.setSeconds(0);
        programada.setTimestampInicio(String.valueOf(fechaNueva.getTime()));

        updateProgramada();
    }

    private void updateProgramada(){
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("timestampInicio", programada.getTimestampInicio());
        referenceProgramadas.child(programada.getFumigacionId()).updateChildren(childUpdates);
    }
}
