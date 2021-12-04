package com.example.fumigabot.firebase;

import androidx.annotation.Nullable;
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
    private int convCantidadQuimicoPorArea;
    private String observaciones;
    private int nivelBateriaInicial;
    private int nivelBateriaFinal;
    private int nivelQuimicoInicial;
    private int nivelQuimicoFinal;
    private String idProgramada;
    private boolean activa;
    private boolean programada;
    private Boolean recurrente;
    private boolean eliminada;

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
        convertirCantidadQuimicoPorArea();
    }

    public String getObservaciones() { return observaciones; }

    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public int getNivelBateriaInicial() { return nivelBateriaInicial; }

    public void setNivelBateriaInicial(int nivel) { this.nivelBateriaInicial = nivel; }

    public int getNivelBateriaFinal() { return nivelBateriaFinal; }

    public void setNivelBateriaFinal(int nivel) { this.nivelBateriaFinal = nivel; }

    public int getNivelQuimicoInicial() { return nivelQuimicoInicial; }

    public void setNivelQuimicoInicial(int nivel) { this.nivelQuimicoInicial = nivel; }

    public int getNivelQuimicoFinal() { return nivelQuimicoFinal; }

    public void setNivelQuimicoFinal(int nivel) { this.nivelQuimicoFinal = nivel; }

    public String getIdProgramada() { return idProgramada; }

    public void setIdProgramada(String idProgramada) { this.idProgramada = idProgramada; }

    public boolean isActiva() { return activa; }

    public void setActiva(boolean valor) { this.activa = valor; }

    public boolean isProgramada() { return programada; }

    public void setProgramada(boolean programada) { this.programada = programada; }

    public Boolean isRecurrente() { return recurrente; }

    public void setRecurrente(Boolean recurrente) { this.recurrente = recurrente; }

    public boolean isEliminada() { return eliminada; }

    public void setEliminada(boolean valor) { this.eliminada = valor; }

    public void convertirCantidadQuimicoPorArea(){
        if(this.cantidadQuimicoPorArea.startsWith("Baja"))
            this.convCantidadQuimicoPorArea = 1;
        else if(this.cantidadQuimicoPorArea.startsWith("Media"))
            this.convCantidadQuimicoPorArea = 2;
        else if(this.cantidadQuimicoPorArea.startsWith("Alta"))
            this.convCantidadQuimicoPorArea = 3;
    }

    public String darFormatoFechaInicio() {
        SimpleDateFormat formateador = new SimpleDateFormat("dd MMMM yyyy");
        Date fechaHoraInicio = new Date(Long.parseLong(timestampInicio));
        String fechaHoraInicioFormateada = formateador.format(fechaHoraInicio);
        return fechaHoraInicioFormateada;
    }

    public String formatoFechaInicioOneTime() {
        SimpleDateFormat formateador = new SimpleDateFormat("E, dd MMMM yyyy");
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

    public String normalizarObservaciones() {
        String mensaje = "";
        switch (this.observaciones) {
            case "fdb":
                mensaje = "El robot detuvo su ejecución debido a que se quedó sin batería.";
                break;
            case "fdq":
                mensaje = "El robot detuvo su ejecución debido a que se quedó sin químico.";
                break;
            case "bnd":
                mensaje = "La fumigación programada no se realizó porque el robot no tenía " +
                        "batería suficiente.";
                break;
            case "qnd":
                mensaje = "La fumigación programada no se realizó porque el robot no tenía " +
                        "químico suficiente.";
                break;
            case "qnc":
                mensaje = "La fumigación programada no se realizó porque el químico que " +
                        "contiene el robot no coincide con el indicado.";
                break;
            case "rf":
                mensaje = "La fumigación programada no se realizó porque el robot se encontraba " +
                        "realizando otra fumigación.";
                break;
            case "ra":
                mensaje = "La fumigación programada no se realizó porque el robot estaba apagado.";
                break;
            default:
                mensaje = "ok";
                break;
        }
        return mensaje;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return //(((Fumigacion)obj).getFumigacionId().equals(this.fumigacionId) ||
                ((Fumigacion)obj).getTimestampInicio().equals(this.timestampInicio);
    }


    /**Para crear la fumigación actual (en curso) en el nodo fumigacionActual del robot
     * */
    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("timestampFin", timestampFin);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", convCantidadQuimicoPorArea);
        hashMap.put("nivelBateriaInicial", nivelBateriaInicial);
        hashMap.put("nivelBateriaFinal", nivelBateriaFinal);
        hashMap.put("nivelQuimicoInicial", nivelQuimicoInicial);
        hashMap.put("nivelQuimicoFinal", nivelQuimicoFinal);

        if(observaciones != null && observaciones != "")
            hashMap.put("observaciones", observaciones);

        return hashMap;
    }


    /** Para crear una instancia de historial en fumigaciones_historial
     * */
    public Map<String, Object> toMapHistorial() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("timestampFin", timestampFin);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", cantidadQuimicoPorArea);
        hashMap.put("nivelBateriaInicial", nivelBateriaInicial);
        hashMap.put("nivelBateriaFinal", nivelBateriaFinal);
        hashMap.put("nivelQuimicoInicial", nivelQuimicoInicial);
        hashMap.put("nivelQuimicoFinal", nivelQuimicoFinal);
        hashMap.put("idProgramada", idProgramada);
        hashMap.put("programada", programada);
        hashMap.put("recurrente", recurrente);
        //agregar días de recurrencia

        if(observaciones != null && observaciones != "")
            hashMap.put("observaciones", observaciones);

        return hashMap;
    }

    /** Para crear una fumigación programada en el nodo de fumigaciones_programadas
     * */
    public Map<String, Object> toMapProgramada() {
        //Crear programadas
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", cantidadQuimicoPorArea);
        hashMap.put("recurrente", recurrente);
        hashMap.put("activa", activa);
        hashMap.put("eliminada", false);
        //agregar la ¿recurrencia?
        return hashMap;
    }
}
