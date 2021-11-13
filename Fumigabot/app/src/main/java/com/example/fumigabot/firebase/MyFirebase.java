package com.example.fumigabot.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyFirebase {
    private static FirebaseDatabase firebaseDatabase;
    private static FirebaseFunctions firebaseFunctions;
    private static String token;

    private MyFirebase(){
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        firebaseFunctions = FirebaseFunctions.getInstance();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.i("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        token = task.getResult();
                        // Log and toast
                        Log.i("FCM", token);
                        firebaseDatabase.getReference("robots/0")
                                .child("token").setValue(token);
                    }
                });
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