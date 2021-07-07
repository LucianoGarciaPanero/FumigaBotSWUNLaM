package com.example.fumigabot.firebase;

import java.security.Timestamp;

public class Fumigacion {

    private Timestamp timestampInicio;
    private Timestamp timestampFin;

    public Fumigacion(){
        // Default constructor required for calls to DataSnapshot.getValue(Robot.class)
    }

    public Fumigacion(Timestamp timestampInicio, Timestamp timestampFin){
        this.timestampInicio = timestampInicio;
        this.timestampFin = timestampFin;
    }

    public Timestamp getTimestampInicio() { return timestampInicio; }

    public void setTimestampInicio(Timestamp timestampInicio) { this.timestampInicio = timestampInicio; }

    public Timestamp getTimestampFin() { return timestampFin; }

    public void setTimestampFin(Timestamp timestampFin) { this.timestampFin = timestampFin; }
}
