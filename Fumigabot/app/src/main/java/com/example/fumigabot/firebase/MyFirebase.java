package com.example.fumigabot.firebase;

import com.google.firebase.database.FirebaseDatabase;

public class MyFirebase {
    private static FirebaseDatabase firebaseDatabase;

    private MyFirebase(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
    }

    public static FirebaseDatabase getInstance(){
        if(firebaseDatabase == null)
            new MyFirebase();
        return firebaseDatabase;
    }
}