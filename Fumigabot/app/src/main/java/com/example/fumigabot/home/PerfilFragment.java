package com.example.fumigabot.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;

/**
 * A simple {@link Fragment} subclass.
 */
public class PerfilFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private TextView textUserEmail;
    private Button btnCerrarSesion;
    private MaterialAlertDialogBuilder alertDialogBuilder;
    private AlertDialog alertDialog;

    public PerfilFragment(){
        // Required empty public constructor
        super(R.layout.fragment_perfil);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i("FILTRO", "QUIMICOS FRAGMENT: onCreate " + SystemClock.elapsedRealtime());
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();

        View vista = getView();
        textUserEmail= vista.findViewById(R.id.textUserEmail);
        textUserEmail.setText(getDatosDeSesion());
        btnCerrarSesion = vista.findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(btnCerrarSesionListener);
    }

    /**
     * Obtiene los datos de sesión (por ahora solo email) de la cuenta Gmail con la que
     * ingresamos anteriormente a la app.
     *
     * @return el email de la cuenta Gmail con la que ingresamos a la app. Puede ser null
     * si no hay una sesión guardada.
     */
    private String getDatosDeSesion() {
        SharedPreferences sp = getActivity().getSharedPreferences(
            String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);

        String datos = sp.getString("userEmail", null);
        return datos;
    }

    private View.OnClickListener btnCerrarSesionListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            inicializarAlertDialog();
        }
    };

    private void inicializarAlertDialog() {
        alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());

        alertDialogBuilder.setMessage("¿Seguro querés cerrar sesión?");

        alertDialogBuilder.setPositiveButton(
            "cerrar sesión", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                cerrarSesion();
            }
        });

        alertDialogBuilder.setNegativeButton(
            "cancelar", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void cerrarSesion() {
        firebaseAuth.signOut();
        borrarDatosDeSesion();
        getActivity().finish();
    }

    /**
     * Borra los datos de sesión (por ahora solo email) de la cuenta Gmail con la que
     * ingresamos anteriormente a la app.
     */
    private void borrarDatosDeSesion() {
        SharedPreferences sp = getActivity().getSharedPreferences(
            String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.clear();
        spEditor.apply();
    }
}
