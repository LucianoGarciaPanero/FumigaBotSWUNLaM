package com.example.fumigabot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int GOOGLE_SIGN_IN_ID = 1001;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceUsers;
    private DatabaseReference referenceRobots;
    private Button btnGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);


        firebaseAuth = FirebaseAuth.getInstance();
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignIn.setOnClickListener(googleSignInListener);

        firebaseDatabase = MyFirebase.getDatabaseInstance();
        referenceUsers = firebaseDatabase.getReference("users");
        referenceRobots = firebaseDatabase.getReference("robots");
    }

    private View.OnClickListener googleSignInListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

            googleSignInClient =
                GoogleSignIn.getClient(getApplicationContext(), googleSignInOptions);
            googleSignInClient.signOut();

            startActivityForResult(googleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_ID);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado de haber ejecutado GoogleSignInApi.getSignInIntent(...)
        if (requestCode == GOOGLE_SIGN_IN_ID) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In exitoso, ahora autenticamos contra Firebase también
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    autenticarEnFirebase(account);
                }
            } catch (ApiException e) {
                // Google Sign In fallido, informamos del error
                Log.w("WTF", "Google sign in failed", e);
            }
        }
    }

    /**
     * Autentica en Firebase una vez hecho el sign in con Gmail. Esto es porque
     * se necesita registrar el inicio de sesión en ambas plataformas.
     *
     * @param account cuenta de Gmail con la que nos logueamos en Google. No puede ser null.
     */
    private void autenticarEnFirebase(GoogleSignInAccount account) {
        AuthCredential credential =
            GoogleAuthProvider.getCredential(account.getIdToken(), null);

        firebaseAuth
            .signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        //guardarDatosDeSesion(account.getEmail(), account.getDisplayName());
                        verificarVinculacionUsuario(account.getEmail(), account.getDisplayName());
                    }
                    else {
                        runOnUiThread(Toast.makeText(
                            getApplicationContext(),
                            "Hubo un error en el login",
                            Toast.LENGTH_SHORT)::show);

                        Log.w("WTF",
                            "autenticarFirebaseConGoogle:failed", task.getException());
                    }
                }
            });
    }

    private void goToVincularDispositivoActivity(String userEmail, String userName) {
        Intent i = new Intent(getApplicationContext(), VincularDispositivoActivity.class);
        i.putExtra("userEmail", userEmail);
        i.putExtra("userName", userName);
        startActivity(i);
        finish();
    }

    private void verificarVinculacionUsuario(String userEmail, String userName) {
        //Opcion 1: estructura simple
        String emailKey = userEmail.substring(0, userEmail.indexOf('@'));

        Task<DataSnapshot> task = referenceUsers.get();

        task.addOnCompleteListener(task1-> {
            String robotId = "";

            if(task1.isSuccessful()){
                for(DataSnapshot robot: task1.getResult().getChildren()){
                    for(DataSnapshot user : robot.getChildren()){
                        if(user.getKey().equals(emailKey)){
                            //es porque tiene un robot en Firebase: tomamos el ID del robot
                            robotId = robot.getKey(); // --> ID del robot
                            break;
                        }
                    }
                }

                if(robotId.isEmpty()){
                    //Nos vamos a Vincular
                    goToVincularDispositivoActivity(userEmail, userName);
                } else {
                    //Guardar SP
                    //Token
                    guardarDatosDeSesion(userEmail, userName, robotId);
                    getToken(emailKey, robotId);

                    goToRobotHomeActivity(robotId);
                }
            }
        });
    }

    private void getToken(String userEmail, String robotId){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Log.i("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                String token = task.getResult();
                /*Map<String, String> newUser = new HashMap<>();
                newUser.put("userEmail", userEmail);
                newUser.put("token", token);
                referenceUsers.child(pin).push().setValue(newUser);*/
                referenceUsers.child(robotId).child(userEmail).setValue(token);
                // Log and toast
                Log.i("FCM", token);
                //return token;
            }
        });
    }

    private void guardarDatosDeSesion(String userEmail, String userName, String robotId) {
        SharedPreferences sp =
                getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();

        spEditor.putString("userEmail", userEmail);
        spEditor.putString("userName", userName);
        spEditor.putString("robotId", robotId);
        spEditor.apply();
    }

    private void goToRobotHomeActivity(String robotId){
        Task<DataSnapshot> task = referenceRobots.child(robotId).get();

        task.addOnCompleteListener(task1-> {
            if(task1.isSuccessful()){
                Robot robot = task1.getResult().getValue(Robot.class);
                Intent i = new Intent(getApplicationContext(), RobotHomeActivity.class);
                i.putExtra("RobotVinculado", robot);
                startActivity(i);
                finish();
            }
        });
    }
}
