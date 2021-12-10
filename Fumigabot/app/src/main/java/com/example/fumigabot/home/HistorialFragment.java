package com.example.fumigabot.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.DetalleEntradaHistorialActivity;
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
public class HistorialFragment extends Fragment {

    private Robot robot;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private int robotId;
    private ArrayList<Fumigacion> listaFumigaciones = new ArrayList<>();
    private Fumigacion fumigacion;
    private TextView textSinFumigaciones;
    private ListView listadoHistorial;
    private EntradaHistorialAdapter adapter;
    private ConstraintLayout layoutSinHistorial;
    private ConstraintLayout layoutListaHistorial;


    public HistorialFragment(){
        // Required empty public constructor
        super(R.layout.fragment_historial);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Transiciones al cambiar de pantallas en la navegación
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        robotId = robot.getRobotId();

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getDatabaseInstance();
        reference = firebaseDatabase.getReference("fumigaciones_historial/" + robotId);
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

        View vista = getView();

        textSinFumigaciones = vista.findViewById(R.id.textSinFumigaciones); // View.INVISIBLE x default
        listadoHistorial = vista.findViewById(R.id.listaEntradaFumigaciones);
        layoutListaHistorial = vista.findViewById(R.id.layoutListaHistorial);
        layoutSinHistorial = vista.findViewById(R.id.layoutSinHistorial);

        cargarVista();
    }

    private void cargarVista() {
        Collections.sort(listaFumigaciones);
        adapter = new EntradaHistorialAdapter(getContext(), listaFumigaciones);
        listadoHistorial.setAdapter(adapter);
        listadoHistorial.setOnItemClickListener(entradaListener);
    }

    private AdapterView.OnItemClickListener entradaListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i = new Intent(getContext(), DetalleEntradaHistorialActivity.class);
            i.putExtra("entradaHistorialSeleccionada",((Fumigacion)listadoHistorial.getItemAtPosition(position)));
            startActivity(i);
        }
    };

    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            listaFumigaciones.clear();

            // Buscamos las fumigaciones en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacion = item.getValue(Fumigacion.class);
                fumigacion.setFumigacionId(item.getKey());
                listaFumigaciones.add(fumigacion);
            }

            if(listaFumigaciones.size() > 0){
                cargarVista();
                layoutSinHistorial.setVisibility(View.GONE);
                layoutListaHistorial.setVisibility(View.VISIBLE);
            }
            else {
                layoutSinHistorial.setVisibility(View.VISIBLE);
                layoutListaHistorial.setVisibility(View.GONE);
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };
}