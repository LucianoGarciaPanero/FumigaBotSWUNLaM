package com.example.fumigabot.firebase;

import java.io.Serializable;

public class Fumigacion implements Comparable {

    private String fumigacionId;
    private String timestampInicio;
    private String timestampFin;

    public Fumigacion(){
        // Default constructor required for calls to DataSnapshot.getValue(Fumigacion.class)
    }

    public Fumigacion (String fumigacionId, String timestampInicio, String timestampFin){
        this.fumigacionId = fumigacionId;
        this.timestampInicio = timestampInicio;
        this.timestampFin = timestampFin;
    }

    public String getFumigacionId() { return fumigacionId; }

    public void setFumigacionId (String fumigacionId) { this.fumigacionId = fumigacionId; }

    public String getTimestampInicio() { return timestampInicio; }

    public void setTimestampInicio (String timestampInicio) { this.timestampInicio = timestampInicio; }

    public String getTimestampFin() { return timestampFin; }

    public void setTimestampFin (String timestampFin) { this.timestampFin = timestampFin; }

    // Ordena ascendentementte según timestampInicio
    @Override
    public int compareTo(Object o) {
        String timestampInicioCmp = ((Fumigacion) o).getTimestampInicio();
        return this.timestampInicio.compareTo(timestampInicioCmp);
    }
}
