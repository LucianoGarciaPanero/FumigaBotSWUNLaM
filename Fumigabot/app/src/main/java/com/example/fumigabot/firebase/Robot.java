package com.example.fumigabot.firebase;

import android.widget.ArrayAdapter;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Robot implements Serializable {
    private int robotId;
    private boolean fumigando;
    private boolean encendido;
    private int bateria;
    private int nivelQuimico;
    private ArrayList<String> quimicosDisponibles = new ArrayList<>();

    public Robot(){
        // Default constructor required for calls to DataSnapshot.getValue(Robot.class)
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
        this.encendido = encendido;
    }

    public int getBateria() {
        return bateria;
    }

    public void setBateria(int bateria) {
        this.bateria = bateria;
    }

    public int getNivelQuimico() {
        return nivelQuimico;
    }

    public void setNivelQuimico(int nivelQuimico) {
        this.nivelQuimico = nivelQuimico;
    }

    public ArrayList<String> getQuimicosDisponibles() {
        return quimicosDisponibles;
    }

    public void setQuimicosDisponibles(ArrayList<String> quimicosDisponibles) {
        this.quimicosDisponibles = quimicosDisponibles;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("fumigando", fumigando);
        return hashMap;
    }
}
