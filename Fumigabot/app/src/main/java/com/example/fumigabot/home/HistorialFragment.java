package com.example.fumigabot.home;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 */
public class HistorialFragment extends Fragment {

    private Robot robot;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private int robotId;
    private ArrayList<Fumigacion> listaFumigaciones = new ArrayList<>();
    private Fumigacion fumigacion;
    private TableLayout tablaHistorial;
    private TextView textSinFumigaciones;
    private ListView listado;
    private EntradaHistorialAdapter adapter;


    private final int SEGUNDOS_MILIS = 1000;
    private final int MINUTOS_MILIS = SEGUNDOS_MILIS * 60;
    private final int HORAS_MILIS = MINUTOS_MILIS * 60;
    //private final int DIAS_MILIS = HORAS_MILIS * 24;


    public HistorialFragment(){
        // Required empty public constructor
        super(R.layout.fragment_historial);
    }

    /*public static HistorialFragment newInstance(Robot robot) {
        HistorialFragment historialFragment = new HistorialFragment();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable("robot", robot);
        historialFragment.setArguments(argumentos);
        return historialFragment;
    }*/

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("FILTRO", "HISTORIAL FRAGMENT: onCreate " + SystemClock.elapsedRealtime());
        //TransitionInflater inflater = TransitionInflater.from(requireContext());
        //setEnterTransition(new MaterialFadeThrough());
        //setExitTransition(new MaterialFadeThrough());
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());


        //---------- LO QUE ESTA EN LA ACTIVITY ORIGINAL ----------------

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        //Obtiene el ID del robot, único dato necesario para conocer sus historiales
        robotId = robot.getRobotId();

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("fumigaciones/" + robotId);
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reference.addValueEventListener(fumigacionesEventListener);

        tablaHistorial = getView().findViewById(R.id.tablaHistorial);
        textSinFumigaciones = getView().findViewById(R.id.textSinFumigaciones); // View.INVISIBLE x default
        listado = getView().findViewById(R.id.listaEntradaFumigaciones);

        cargarVista();
    }

    private void cargarVista() {
        Collections.sort(listaFumigaciones);
        adapter = new EntradaHistorialAdapter(getContext(), listaFumigaciones);
        listado.setAdapter(adapter);
        //acá podemos agregar un listener para on item click
    }

    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Limpiamos todas las fumigaciones anteriores
            // ya que si se agrega o modifica una, va a cargar repetidas
            listaFumigaciones.clear();

            // Buscamos las fumigaciones en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacion = item.getValue(Fumigacion.class);
                fumigacion.setFumigacionId(item.getKey());
                listaFumigaciones.add(fumigacion);
            }

            if(listaFumigaciones.size() > 0){
                // Ordena la lista descendentemente según timestampInicio
                /*Collections.sort(listaFumigaciones);
                generarTablaFumigaciones(listaFumigaciones);*/
                cargarVista();
            }
            else {
                listado.setVisibility(View.INVISIBLE);
                textSinFumigaciones.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };
}
