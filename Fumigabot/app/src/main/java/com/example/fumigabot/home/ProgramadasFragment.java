package com.example.fumigabot.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.EntradaHistorialAdapter;
import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProgramadasFragment extends Fragment {

    private Robot robot;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private int robotId;
    private ArrayList<Fumigacion> listaProgramadas = new ArrayList<>();
    private Fumigacion fumigacion;
    private TextView textSinFumigaciones;
    private ListView listadoProgramadas;
    private EntradaHistorialAdapter adapter;


    public ProgramadasFragment(){
        // Required empty public constructor
        super(R.layout.fragment_programadas);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Transiciones al cambiar de pantallas en la navegación
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        //Obtiene el ID del robot, único dato necesario para conocer sus historiales
        robotId = robot.getRobotId();

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("fumigaciones_programadas/" + robotId);
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_programadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
/*
        reference.addValueEventListener(fumigacionesEventListener);

        View vista = getView();

        textSinFumigaciones = vista.findViewById(R.id.textSinFumigaciones); // View.INVISIBLE x default
        listadoHistorial = vista.findViewById(R.id.listaEntradaFumigaciones);

        cargarVista();*/
    }

    private void cargarVista() {
        /*Collections.sort(listaFumigaciones);
        adapter = new EntradaHistorialAdapter(getContext(), listaFumigaciones);
        listadoHistorial.setAdapter(adapter);*/
        //acá podemos agregar un listener para on item click
    }

    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Limpiamos todas las fumigaciones anteriores
            // ya que si se agrega o modifica una, va a cargar repetidas
            /*listaFumigaciones.clear();

            // Buscamos las fumigaciones en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacion = item.getValue(Fumigacion.class);
                fumigacion.setFumigacionId(item.getKey());
                listaFumigaciones.add(fumigacion);
            }

            if(listaFumigaciones.size() > 0){
                cargarVista();
            }
            else {
                listadoHistorial.setVisibility(View.INVISIBLE);
                textSinFumigaciones.setVisibility(View.VISIBLE);
            }*/
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };
}