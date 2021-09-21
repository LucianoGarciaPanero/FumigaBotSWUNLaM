package com.example.fumigabot.firebase;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Fumigacion implements Comparable, Serializable {

    private String fumigacionId;
    private String timestampInicio;
    private String timestampFin;
    private String quimicoUtilizado;
    private String cantidadQuimicoPorArea;
    private String observaciones;
    private boolean programada;
    private boolean recurrente;

    private final int SEGUNDOS_MILIS = 1000;
    private final int MINUTOS_MILIS = SEGUNDOS_MILIS * 60;
    private final int HORAS_MILIS = MINUTOS_MILIS * 60;
    private final int DIAS_MILIS = HORAS_MILIS * 24;

    public Fumigacion(){
        // Default constructor required for calls to DataSnapshot.getValue(Fumigacion.class)
    }

    public String getFumigacionId() { return fumigacionId; }

    public void setFumigacionId (String fumigacionId) { this.fumigacionId = fumigacionId; }

    public String getTimestampInicio() { return timestampInicio; }

    public void setTimestampInicio (String timestampInicio) { this.timestampInicio = timestampInicio; }

    public String getTimestampFin() { return timestampFin; }

    public void setTimestampFin (String timestampFin) { this.timestampFin = timestampFin; }

    public String getQuimicoUtilizado() { return quimicoUtilizado; }

    public void setQuimicoUtilizado(String quimicoUtilizado) { this.quimicoUtilizado = quimicoUtilizado; }

    public String getCantidadQuimicoPorArea() { return cantidadQuimicoPorArea; }

    public void setCantidadQuimicoPorArea(String cantidadQuimicoPorArea) {
        this.cantidadQuimicoPorArea = cantidadQuimicoPorArea;
    }

    public String getObservaciones() { return observaciones; }

    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public boolean isProgramada() { return programada; }

    public void setProgramada(boolean programada) { this.programada = programada; }

    public boolean isRecurrente() { return recurrente; }

    public void setRecurrente(boolean programada) { this.recurrente = recurrente; }

    public String darFormatoFechaInicio() {
        SimpleDateFormat formateador = new SimpleDateFormat("dd MMMM yyyy");
        Date fechaHoraInicio = new Date(Long.parseLong(timestampInicio));
        String fechaHoraInicioFormateada = formateador.format(fechaHoraInicio);
        return fechaHoraInicioFormateada;
    }

    public String getHoraInicio() {
        SimpleDateFormat formateador = new SimpleDateFormat("HH:mm");
        Date horaInicio = new Date(Long.parseLong(timestampInicio));
        String horaInicioFormateada = formateador.format(horaInicio);
        return horaInicioFormateada;
    }

    public String getHoraFin() {
        SimpleDateFormat formateador = new SimpleDateFormat("HH:mm");
        Date horaFin = new Date(Long.parseLong(timestampFin));
        String horaFinFormateada = formateador.format(horaFin);
        return horaFinFormateada;
    }

    public String calcularDuracion() {

        long diferencia = Long.parseLong(timestampFin) - Long.parseLong(timestampInicio);

        //Calculamos el paso del tiempo real
        long dias = diferencia / DIAS_MILIS;
        diferencia %= DIAS_MILIS;

        long horas = diferencia / HORAS_MILIS;
        diferencia %= HORAS_MILIS;

        long minutos = diferencia / MINUTOS_MILIS;
        diferencia &= MINUTOS_MILIS;

        long segundos = diferencia / SEGUNDOS_MILIS;


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

    // Ordena descendentemente según timestampInicio
    @Override
    public int compareTo(Object o) {
        String timestampInicioCmp = ((Fumigacion) o).getTimestampInicio();
        return timestampInicioCmp.compareTo(this.timestampInicio);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("timestampFin", timestampFin);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", cantidadQuimicoPorArea);

        Boolean esProgramada = programada;
        if(esProgramada != null)
            hashMap.put("programada", programada);
        if(observaciones != null)
            hashMap.put("observaciones", observaciones);
        return hashMap;
    }

    public Map<String, Object> toMapProgramada() {
        //Crear programadas
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", cantidadQuimicoPorArea);
        hashMap.put("recurrente", recurrente);
        return hashMap;
    }
}
