package com.example.fumigabot.firebase;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Robot {
    private int robotId;
    private boolean fumigando;

    public Robot(){
        // Default constructor required for calls to DataSnapshot.getValue(Robot.class)
    }

    public Robot(int robotId, boolean fumigando){
        this.robotId = robotId;
        this.fumigando = fumigando;
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

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("fumigando", fumigando);
        return hashMap;
    }
}
