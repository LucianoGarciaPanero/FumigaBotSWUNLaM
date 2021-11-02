package com.example.fumigabot.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;

public class MyFirebase {
    private static FirebaseDatabase firebaseDatabase;
    private static FirebaseFunctions firebaseFunctions;

    private MyFirebase(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        firebaseFunctions = FirebaseFunctions.getInstance();
    }

    public static FirebaseDatabase getDatabaseInstance(){
        if(firebaseDatabase == null && firebaseFunctions == null)
            new MyFirebase();
        return firebaseDatabase;
    }

    public static FirebaseFunctions getFunctionsInstance(){
        if(firebaseFunctions == null && firebaseFunctions == null)
            new MyFirebase();
        return firebaseFunctions;
    }
}