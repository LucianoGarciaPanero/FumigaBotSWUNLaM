package com.example.fumigabot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;

import java.text.SimpleDateFormat;

public class DetalleEntradaHistorialActivity extends AppCompatActivity {

    private Fumigacion fumigacion;
    private TextView fecha;
    private TextView quimicoUsado;
    private TextView cantidadUsada;
    private TextView duracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_entrada_historial);

        fumigacion = (Fumigacion) getIntent().getSerializableExtra("entradaHistorialSeleccionada");

        inicializarControles();
        cargarDatos();
    }

    private void inicializarControles(){
        fecha = findViewById(R.id.fechaFumigacion);
        quimicoUsado = findViewById(R.id.quimicoUsado);
        cantidadUsada = findViewById(R.id.cantidadUsada);
        duracion = findViewById(R.id.duracionDetalle);
    }

    private void cargarDatos(){
        fecha.setText(fumigacion.darFormatoFechaInicio());
        quimicoUsado.setText(fumigacion.getQuimicoUtilizado());
        cantidadUsada.setText(fumigacion.getCantidadQuimicoPorArea());
        duracion.setText(fumigacion.calcularDuracion());

        String cantidad = fumigacion.getCantidadQuimicoPorArea();
        if(cantidad.startsWith("A"))
            cantidad = "Alta";
        else if(cantidad.startsWith("B"))
            cantidad = "Baja";
        else
            cantidad = "Media";

        cantidadUsada.setText(cantidad);

        //if(fumigacion.isProgramada())

        //if(fumigacion.getObservaciones() != null)

    }
}
