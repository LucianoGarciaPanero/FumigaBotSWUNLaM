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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class PerfilFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private TextView textUserEmail;
    private TextView textUserName;
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
        textUserEmail = vista.findViewById(R.id.textUserEmail);
        textUserName = vista.findViewById(R.id.textUserName);
        textUserEmail.setText(getDatosDeSesion().get("userEmail"));
        textUserName.setText(getDatosDeSesion().get("userName"));
        btnCerrarSesion = vista.findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(btnCerrarSesionListener);
    }

    private Map<String, String> getDatosDeSesion() {
        Map<String, String> datosDeSesion = new HashMap<>();

        SharedPreferences sp = getActivity()
            .getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);

        String userEmail = sp.getString("userEmail", null);
        String userName = sp.getString("userName", null);
        datosDeSesion.put("userEmail", userEmail);
        datosDeSesion.put("userName", userName);

        return datosDeSesion;
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

    private void borrarDatosDeSesion() {
        SharedPreferences sp = getActivity().getSharedPreferences(
            String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.clear();
        spEditor.apply();
    }
}
