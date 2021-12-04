package com.example.fumigabot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;

import java.sql.Date;
import java.text.SimpleDateFormat;

public class DetalleEntradaHistorialActivity extends AppCompatActivity {

    private Fumigacion fumigacion;
    private TextView fecha;
    private TextView quimicoUsado;
    private TextView cantidadUsada;
    private TextView duracion;
    private TextView diasRecurrencia;
    private TextView txtConsumoQuimico;
    private TextView txtConsumoBateria;
    private ImageView imgAlertaObservaciones;
    private TextView txtObservaciones;
    private ConstraintLayout layoutObservaciones;

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
        diasRecurrencia = findViewById(R.id.txtDiasRecurrencia);
        txtConsumoQuimico = findViewById(R.id.txtConsumoQuimico);
        txtConsumoBateria = findViewById(R.id.txtConsumoBateria);
        imgAlertaObservaciones = findViewById(R.id.imgAlertaObservaciones);
        txtObservaciones = findViewById(R.id.txtObservaciones);
        layoutObservaciones = findViewById(R.id.layoutObservaciones);
    }

    private void cargarDatos(){
        fecha.setText(fumigacion.darFormatoFechaInicio() + " a las " + fumigacion.getHoraInicio());
        quimicoUsado.setText(fumigacion.getQuimicoUtilizado());
        cantidadUsada.setText(fumigacion.getCantidadQuimicoPorArea());

        String cantidad = fumigacion.getCantidadQuimicoPorArea();
        if(cantidad.startsWith("A"))
            cantidad = "Alta";
        else if(cantidad.startsWith("B"))
            cantidad = "Baja";
        else
            cantidad = "Media";


        setearDiasRecurrencia();
        txtConsumoBateria.setText(calcularConsumoBateria());
        txtConsumoQuimico.setText(calcularConsumoQuimico());

        cantidadUsada.setText(cantidad);
        setearDuracion();
        setearObservaciones();
    }

    private void setearDiasRecurrencia(){
        if(fumigacion.isRecurrente() == null) {
            //instantanea
            diasRecurrencia.setVisibility(View.GONE);
            return;
        }

        diasRecurrencia.setVisibility(View.VISIBLE);

        if(fumigacion.isRecurrente() == false) {
            //one time
            diasRecurrencia.setText("Programada");
            return;
        }

        String texto = "Programada, todos los ";
        Date fecha = new Date(Long.parseLong(fumigacion.getTimestampInicio()));
        switch(fecha.getDay()) {
            case 0:
                texto += "domingos";
                break;
            case 1:
                texto += "lunes";
                break;
            case 2:
                texto += "martes";
                break;
            case 3:
                texto += "miércoles";
                break;
            case 4:
                texto += "jueves";
                break;
            case 5:
                texto += "viernes";
                break;
            case 6:
                texto += "sábados";
                break;
            default:
                break;
        }
        diasRecurrencia.setText(texto);
    }

    private String calcularConsumoBateria(){
        String texto = "";

        if(fumigacion.getTimestampFin().equals("0")){
            texto = "N/A";
        } else {
            texto = (fumigacion.getNivelBateriaInicial() - fumigacion.getNivelBateriaFinal()) + "%";
        }
        return texto;
    }

    private String calcularConsumoQuimico(){
        String texto = "";

        if(fumigacion.getTimestampFin().equals("0")){
            texto = "N/A";
        } else {
            texto = (fumigacion.getNivelQuimicoInicial() - fumigacion.getNivelQuimicoFinal()) + "%";
        }
        return texto;
    }

    private void setearObservaciones(){
        String observaciones = fumigacion.normalizarObservaciones();
        if(observaciones.equals("ok")){
            layoutObservaciones.setVisibility(View.GONE);
        } else {
            layoutObservaciones.setVisibility(View.VISIBLE);
            txtObservaciones.setText(observaciones);
        }
    }

    private void setearDuracion(){
        if(fumigacion.getTimestampFin().equals("0")){
            duracion.setText("N/A");
        } else {
            duracion.setText(fumigacion.calcularDuracion());
        }
    }
}
