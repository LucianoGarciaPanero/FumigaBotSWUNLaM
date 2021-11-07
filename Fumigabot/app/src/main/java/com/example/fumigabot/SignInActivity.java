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

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int GOOGLE_SIGN_IN_ID = 1001;
    private Button btnGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_sign_in);
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignIn.setOnClickListener(googleSignInListener);
    }

    /**
     * Obtiene los datos de sesión (por ahora solo email) de la cuenta Gmail con la que
     * ingresamos anteriormente a la app.
     *
     * @return el email de la cuenta Gmail con la que ingresamos a la app. Puede ser null
     * si no hay una sesión guardada.
     */
    private String getDatosDeSesion() {
        SharedPreferences sp =
            getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        String datos = sp.getString("userEmail", null);

        return datos;
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
                runOnUiThread(Toast.makeText(
                    getApplicationContext(),
                    "Hubo un error en el login",
                    Toast.LENGTH_SHORT)::show);
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
                        guardarDatosDeSesion(account.getEmail());
                        goToVincularDispositivoActivity();
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

    /**
     * Guarda los datos de sesión (por ahora solo email) de la cuenta Gmail con la que ingresamos,
     * para recuperarlos en un próximo inicio de la app.
     *
     * @param userEmail el email de la cuenta Gmail con la que iniciamos sesión. No puede ser null.
     */
    private void guardarDatosDeSesion(String userEmail) {
        SharedPreferences sp =
                getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("userEmail", userEmail);
        spEditor.apply();
    }

    private void goToVincularDispositivoActivity() {
        Intent i = new Intent(getApplicationContext(), SplashActivity.class);
        i.putExtra("userEmail", getDatosDeSesion());
        startActivity(i);
        finish();
    }
}
