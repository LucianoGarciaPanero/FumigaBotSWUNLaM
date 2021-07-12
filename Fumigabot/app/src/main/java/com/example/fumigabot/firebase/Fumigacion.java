package com.example.fumigabot.firebase;

import java.io.Serializable;

public class Fumigacion implements Serializable {

    private long timestampInicio;
    private long timestampFin;

    public Fumigacion(){
        // Default constructor required for calls to DataSnapshot.getValue(Fumigacion.class)
    }

    public Fumigacion (long timestampInicio, long timestampFin){
        this.timestampInicio = timestampInicio;
        this.timestampFin = timestampFin;
    }

    public long getTimestampInicio() { return timestampInicio; }

    public void setTimestampInicio (long timestampInicio) { this.timestampInicio = timestampInicio; }

    public long getTimestampFin() { return timestampFin; }

    public void setTimestampFin (long timestampFin) { this.timestampFin = timestampFin; }
}
