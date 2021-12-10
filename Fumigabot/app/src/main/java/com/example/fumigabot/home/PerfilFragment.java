package com.example.fumigabot.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.R;
import com.example.fumigabot.firebase.MyFirebase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class PerfilFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private TextView textUserEmail;
    private TextView textUserName;
    private ImageView userPhoto;
    private Button btnCerrarSesion;
    private Button btnDesvincularRobot;
    private TextView txtIDRobot;
    private MaterialAlertDialogBuilder alertDialogBuilder;
    private AlertDialog alertDialog;
    private DatabaseReference referenceUsers;

    private String userEmail;

    public PerfilFragment(){
        // Required empty public constructor
        super(R.layout.fragment_perfil);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        FirebaseUser user = firebaseAuth.getCurrentUser();
        //Log.i("test", "url de la foto: " + user.getPhotoUrl().toString());

        View vista = getView();
        userPhoto = vista.findViewById(R.id.userPhoto);
        Picasso.get().load(user.getPhotoUrl()).into(userPhoto);
        textUserEmail = vista.findViewById(R.id.textUserEmail);
        textUserName = vista.findViewById(R.id.textUserName);
        userEmail = getDatosDeSesion().get("userEmail");
        textUserEmail.setText(userEmail);
        textUserName.setText(getDatosDeSesion().get("userName"));
        btnCerrarSesion = vista.findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(btnCerrarSesionListener);
        txtIDRobot = vista.findViewById(R.id.txtIDRobot);
        txtIDRobot.setText("ID de Robot: " + getDatosDeSesion().get("robotId"));
        btnDesvincularRobot = vista.findViewById(R.id.btnDesvincularRobot);
        btnDesvincularRobot.setOnClickListener(desvincularRobotListener);

        referenceUsers = MyFirebase.getDatabaseInstance().getReference("users/" + getDatosDeSesion().get("robotId"));
    }

    private Map<String, String> getDatosDeSesion() {
        Map<String, String> datosDeSesion = new HashMap<>();

        SharedPreferences sp = getActivity()
            .getSharedPreferences(String.valueOf(R.string.sp_datos_de_sesion), Context.MODE_PRIVATE);

        String userEmail = sp.getString("userEmail", null);
        String userName = sp.getString("userName", null);
        String robotId = sp.getString("robotId", null);
        datosDeSesion.put("userEmail", userEmail);
        datosDeSesion.put("userName", userName);
        datosDeSesion.put("robotId", robotId);

        return datosDeSesion;
    }

    private View.OnClickListener desvincularRobotListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
            alertDialogBuilder.setMessage("¿Seguro querés desvincular el robot?");

            alertDialogBuilder.setPositiveButton(
                    "desvincular", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            desvincularRobot();
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
    };

    private void desvincularRobot(){
        //set value en null en users/id robot/mail
        String emailKey = userEmail.substring(0, userEmail.indexOf('@'));
        referenceUsers.child(emailKey).setValue(null);
        cerrarSesion();
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
        /*Intent i = new Intent(getContext(), SplashActivity.class);
        startActivity(i);*/
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
