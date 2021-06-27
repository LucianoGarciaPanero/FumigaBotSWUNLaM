package com.example.fumigabot.firebase;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Robot implements Serializable {
    private int robotId;
    private boolean fumigando;
    private boolean encendido;
    private int bateria;

    public Robot(){
        // Default constructor required for calls to DataSnapshot.getValue(Robot.class)
    }

    public Robot(int robotId, boolean fumigando, boolean encendido, int bateria){
        this.robotId = robotId;
        this.fumigando = fumigando;
        this.encendido = encendido;
        this.bateria = bateria;
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public boolean isFumigando() {
        return fumigando;
    }

    public void setFumigando(boolean fumigando) {
        this.fumigando = fumigando;
    }

    public boolean isEncendido() {
        return encendido;
    }

    public void setEncendido(boolean encendido) {
        this.encendido= encendido;
    }

    public int getBateria() {
        return bateria;
    }

    public void setBateria(int bateria) {
        this.bateria= bateria;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("fumigando", fumigando);
        hashMap.put("encendido", encendido);
        hashMap.put("bateria", bateria);
        return hashMap;
    }
}
