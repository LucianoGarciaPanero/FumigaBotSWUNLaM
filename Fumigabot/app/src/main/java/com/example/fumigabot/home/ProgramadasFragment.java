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
import androidx.fragment.app.Fragment;

import com.example.fumigabot.DetalleEntradaHistorialActivity;
import com.example.fumigabot.DetalleFumigacionProgramadaActivity;
import com.example.fumigabot.EntradaHistorialAdapter;
import com.example.fumigabot.EntradaProgramadaAdapter;
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
    private Fumigacion fumigacionProgramada;
    private TextView textSinFumigaciones;
    private ListView listadoProgramadas;
    private EntradaProgramadaAdapter adapter;


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
        firebaseDatabase = MyFirebase.getDatabaseInstance();
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

        reference.addValueEventListener(fumigacionesEventListener);

        View vista = getView();

        textSinFumigaciones = vista.findViewById(R.id.textSinFumigaciones); // View.INVISIBLE x default
        listadoProgramadas = vista.findViewById(R.id.listaFumigacionesProgramadas);

        cargarVista();
    }

    private void cargarVista() {
        Collections.sort(listaProgramadas);
        adapter = new EntradaProgramadaAdapter(getContext(), robot.getRobotId(), listaProgramadas);
        listadoProgramadas.setAdapter(adapter);
        //acá podemos agregar un listener para on item click
        listadoProgramadas.setOnItemClickListener(entradaListener);
    }

    private AdapterView.OnItemClickListener entradaListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i = new Intent(getContext(), DetalleFumigacionProgramadaActivity.class);
            i.putExtra("fumigacionProgramadaSeleccionada",((Fumigacion)listadoProgramadas.getItemAtPosition(position)));
            i.putExtra("idRobot", robot.getRobotId());
            startActivity(i);
        }
    };

    private ValueEventListener fumigacionesEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            listaProgramadas.clear();

            // Buscamos las fumigaciones programadas en Firebase
            for(DataSnapshot item : dataSnapshot.getChildren()) {
                fumigacionProgramada = item.getValue(Fumigacion.class);
                if(!fumigacionProgramada.isEliminada()){
                    fumigacionProgramada.setFumigacionId(item.getKey());
                    listaProgramadas.add(fumigacionProgramada);
                }
            }

            if(listaProgramadas.size() > 0){
                cargarVista();
            }
            else {
                listadoProgramadas.setVisibility(View.INVISIBLE);
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