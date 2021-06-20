package com.example.fumigabot.firebase;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Robot {
    private int robotId;
    private boolean fumigar;

    public Robot(){
        // Default constructor required for calls to DataSnapshot.getValue(Robot.class)
    }

    public Robot(int robotId, boolean fumigar){
        this.robotId = robotId;
        this.fumigar = fumigar;
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public boolean getFumigar() {
        return fumigar;
    }

    public void setFumigar(boolean fumigar) {
        this.fumigar = fumigar;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("fumigar", fumigar);

        return hashMap;
    }
}
