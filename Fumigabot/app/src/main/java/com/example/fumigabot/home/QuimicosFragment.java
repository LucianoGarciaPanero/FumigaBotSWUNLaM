package com.example.fumigabot.home;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.EntradaHistorialAdapter;
import com.example.fumigabot.QuimicosAdapter;
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
public class QuimicosFragment extends Fragment {

    private Robot robot;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private int robotId;
    private ArrayList<String> listaQuimicos = new ArrayList<>();
    private TableLayout tablaQuimicos;
    private TextView textSinQuimicos;
    private ListView listado;
    private QuimicosAdapter adapter;
    private EditText txtNuevoQuimico;
    private Button btnAgregarQuimico;

    public QuimicosFragment(){
        // Required empty public constructor
        super(R.layout.fragment_quimicos);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("FILTRO", "QUIMICOS FRAGMENT: onCreate " + SystemClock.elapsedRealtime());
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());


        //---------- LO QUE ESTA EN LA ACTIVITY ORIGINAL ----------------

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        //Obtiene el ID del robot, único dato necesario para conocer sus historiales
        //robotId = robot.getRobotId();

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quimicos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reference.addValueEventListener(robotEventListener);

        tablaQuimicos= getView().findViewById(R.id.tablaQuimicos);
        textSinQuimicos= getView().findViewById(R.id.textSinQuimicos);
        btnAgregarQuimico = getView().findViewById(R.id.btnAgregarQuimico);
        btnAgregarQuimico.setOnClickListener(btnAgregarQuimicoListener);
        listado = getView().findViewById(R.id.listaEntradaQuimicos);

        cargarVista();
    }

    private void cargarVista() {
        adapter = new QuimicosAdapter(getContext(), listaQuimicos);
        listado.setAdapter(adapter);
        //acá podemos agregar un listener para on item click
    }

    private View.OnClickListener btnAgregarQuimicoListener = v -> {
        String nuevoQuimico = txtNuevoQuimico.getText().toString();
        robot.getQuimicosDisponibles().add(nuevoQuimico);
        updateRobot(robot);
        txtNuevoQuimico.setText("");
        Toast.makeText(getContext(), "¡Químico agregado!", Toast.LENGTH_SHORT).show();
    };

    public void updateRobot(Robot robot) {
        reference.child("quimicosDisponibles").setValue(robot.getQuimicosDisponibles());
    }

    private ValueEventListener robotEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Limpiamos todo para que no agregue repetidas
            listaQuimicos.clear();

            // Buscamos las fumigaciones en Firebase
            //robot = dataSnapshot.child(Integer.toString(robot.getRobotId())).getValue(Robot.class);
            robot = dataSnapshot.getValue(Robot.class);
            listaQuimicos = robot.getQuimicosDisponibles();

            if(listaQuimicos.size() > 0){
                cargarVista();
            }
            else {
                listado.setVisibility(View.INVISIBLE);
                textSinQuimicos.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    @Override
    public void onDestroy() {
        reference.removeEventListener(robotEventListener);
        super.onDestroy();
    }
}
