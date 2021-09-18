package com.example.fumigabot.home;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class InicioFragment extends Fragment {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceFumigacion;
    private LinearLayout layoutExpandible;
    private Button botonExpandir;
    private MaterialCardView cardViewEstado;
    private MaterialCardView cardViewFumigacion;
    private TextView estadoRobot;
    private TextView textEstadoFumigacion;
    private TextView infoBateria;
    private TextView textBateria;
    private ImageView imagenBateria;
    private TextView infoNivelQuimico;
    private TextView textNivelQuimico;
    private ImageView imagenQuimico;
    private String mensajeInfoBateria;
    private String mensajeInfoNivelQuimico;
    private Button btnIniciarFumigacion;
    private Chronometer cronometro;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;
    private Robot robot;
    private Fumigacion fumigacion;
    private TextInputLayout listaQuimicos;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterListaQuimicos;
    private TextInputLayout listaCantidadArea;
    private AutoCompleteTextView autoCompleteTextView2;
    private ArrayAdapter<String> adapterListaCantidadArea;

    private boolean isFumigandoAnterior;
    private int cantQuimicosAnterior;

    private final int BATERIA_NIVEL_ALTO = 40;
    private final int BATERIA_NIVEL_MODERADO = 15;
    private final int BATERIA_NIVEL_BAJO = 5;
    private final int BATERIA_PROBLEMATICA = -1;
    private final int QUIMICO_NIVEL_ALTO = 40;
    private final int QUIMICO_NIVEL_MODERADO = 20;
    private final int QUIMICO_PROBLEMATICO = -1;
    private final int ROBOT_APAGADO = 1;
    private final int ROBOT_ENCENDIDO = 2;
    private final int ROBOT_OCUPADO = 3;

    public InicioFragment() {
        // Required empty public constructor
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Transiciones en los cambios de fragmento
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());

        //Recibimos los datos pasados en el bundle
        robot = (Robot)getArguments().getSerializable("RobotVinculado");
        //Instancia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        //referencia de los robots
        referenceRobot = firebaseDatabase.getReference("robots");
        //Para que se mantenga sincronizado offline
        referenceRobot.keepSynced(true);
        referenceRobot.addValueEventListener(robotValueEventListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        layoutExpandible = vista.findViewById(R.id.layoutExpandible);
        cardViewEstado = vista.findViewById(R.id.cardEstadoRobot);
        /*botonExpandir = vista.findViewById(R.id.botonExpandir);
        botonExpandir.setOnClickListener(botonExpandirListener);*/

        cardViewFumigacion = vista.findViewById(R.id.cardIniciarFumigacion);

        estadoRobot = vista.findViewById(R.id.estadoRobot);
        textEstadoFumigacion = vista.findViewById(R.id.textEstadoFumigacion);
        infoBateria = vista.findViewById(R.id.infoBateria);
        textBateria = vista.findViewById(R.id.porcentajeBateria);
        imagenBateria = vista.findViewById(R.id.imagenBateria);
        infoNivelQuimico = vista.findViewById(R.id.infoNivelQuimico);
        textNivelQuimico = vista.findViewById(R.id.porcentajeQuimico);
        imagenQuimico = vista.findViewById(R.id.imagenQuimico);

        listaQuimicos = vista.findViewById(R.id.listaQuimicos);
        autoCompleteTextView = vista.findViewById(R.id.autoCompleteTextView);

        listaCantidadArea = vista.findViewById(R.id.listaCantidadArea);
        autoCompleteTextView2 = vista.findViewById(R.id.autoCompleteTextView2);

        btnIniciarFumigacion = vista.findViewById(R.id.btnIniciarFumigacion);
        btnIniciarFumigacion.setOnClickListener(btnIniciarFumigacionListener);

        cronometro = vista.findViewById(R.id.Cronometro);
    }

    @Override
    public void onStart() {
        super.onStart();

        isFumigandoAnterior = robot.isFumigando(); // cuestionable
        cantQuimicosAnterior = robot.getQuimicosDisponibles().size(); // cuestionable

        //referencia de las fumigaciones para empezar a guardarlas
        referenceFumigacion = firebaseDatabase.getReference("fumigaciones/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceFumigacion.keepSynced(true);

        configurarAdapterListaQuimicos();
        configurarAdapterListaCantidadPorArea();
    }
/*
    private View.OnClickListener botonExpandirListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(layoutExpandible.getVisibility()==View.GONE){
                TransitionManager.beginDelayedTransition(cardViewEstado, new AutoTransition());
                TransitionManager.beginDelayedTransition(cardViewFumigacion, new AutoTransition());
                layoutExpandible.setVisibility(View.VISIBLE);
                //cambiar boton
                botonExpandir.setText("-");
            }
            else{
                TransitionManager.beginDelayedTransition(cardViewEstado, new AutoTransition());
                TransitionManager.beginDelayedTransition(cardViewFumigacion, new AutoTransition());
                layoutExpandible.setVisibility(View.GONE);
                //cambiar boton
                botonExpandir.setText("+");
            }
        }
    };*/

     private View.OnClickListener btnIniciarFumigacionListener = v -> inicializarAlertDialog();


    public void inicializarAlertDialog() {
        builder = new MaterialAlertDialogBuilder(getContext());

        String titleAlertDialog;
        String accion;

        if(!robot.isFumigando()) {
            titleAlertDialog = "iniciar una ";
            accion = "iniciar";
        }
        else {
            titleAlertDialog = "detener la ";
            accion = "detener";
        }

        builder.setMessage("¿Seguro querés " + titleAlertDialog + "fumigación?");

        builder.setPositiveButton(accion, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!robot.isEncendido()) {
                    builder.setMessage("El dispositivo se encuentra apagado");
                    return;
                }
                else {
                    isFumigandoAnterior = robot.isFumigando(); // cuestionable
                    cantQuimicosAnterior = robot.getQuimicosDisponibles().size(); // cuestionable

                    if (!robot.isFumigando()) {
                        robot.setFumigando(true);
                        robot.convertirCantidadQuimicoPorArea(autoCompleteTextView2.getText().toString());
                        iniciarFumigacion();
                    }
                    else {
                        robot.setFumigando(false);
                        detenerFumigacion();
                    }

                    updateRobot(robot);
                }
                determinarEstadoRobot(robot);
            }
        });

        builder.setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    public void determinarEstadoRobot(Robot robot){
        String porcentajeBateria;
        String porcentajeNivelQuimico;
        boolean statusBateria = false;
        boolean statusNivelQuimico = false;
        int fumigando = View.INVISIBLE;
        int bateria = robot.getBateria();
        int nivelQuimico = robot.getNivelQuimico();

        if(robot.isEncendido()) {
            statusBateria = verificarBateria(bateria);
            statusNivelQuimico = verificarNivelQuimico(nivelQuimico);

            if(bateria == -1)
                porcentajeBateria = "";
                //porcentajeBateria = "Batería: con problemas";
            else
                porcentajeBateria = bateria + "%";
                //porcentajeBateria = "Batería: " + bateria + "%";

            if(nivelQuimico == -1)
                porcentajeNivelQuimico = "";
                //porcentajeNivelQuimico = "Depósito de químico con problemas";
            else
                porcentajeNivelQuimico = nivelQuimico + "%";
                //porcentajeNivelQuimico = "Nivel químico: " + nivelQuimico + "%";


            listaQuimicos.setVisibility(View.VISIBLE);

            if(robot.isFumigando()) {
                btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
                fumigando = View.VISIBLE;
                definirEstadoRobot(ROBOT_OCUPADO, "Fumigando...");
            }
            else {
                //estado = "ESPERANDO ÓRDENES...";
                btnIniciarFumigacion.setText("FUMIGAR");
                fumigando = View.INVISIBLE;
                definirEstadoRobot(ROBOT_ENCENDIDO, "Encendido");
            }
        }
        else {
            porcentajeBateria = porcentajeNivelQuimico = "";
            definirEstadoRobot(ROBOT_APAGADO, "Apagado");
            mensajeInfoBateria = "Encender el robot para comenzar";
            mensajeInfoNivelQuimico = "";
            infoBateria.setBackgroundColor(R.attr.backgroundColor);
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            infoNivelQuimico.setBackgroundColor(R.attr.backgroundColor);
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
            listaQuimicos.setEnabled(false);
            listaCantidadArea.setEnabled(false);

            btnIniciarFumigacion.setText("FUMIGAR");
        }

        infoBateria.setText(mensajeInfoBateria);
        textBateria.setText(porcentajeBateria);
        infoNivelQuimico.setText(mensajeInfoNivelQuimico);
        textNivelQuimico.setText(porcentajeNivelQuimico);
        textEstadoFumigacion.setVisibility(fumigando);
        cronometro.setVisibility(fumigando);

        if(statusBateria && statusNivelQuimico)
            habilitarFumigacion(true);
        else
            habilitarFumigacion(false);
    }

    private void habilitarFumigacion(boolean valor){
        btnIniciarFumigacion.setEnabled(valor);
        listaQuimicos.setEnabled(valor);
        listaCantidadArea.setEnabled(valor);
    }

    private void definirEstadoRobot(int estado, String desc){
        estadoRobot.setText(desc);

        switch (estado){
            case ROBOT_ENCENDIDO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devEncendido));
                return;

            case ROBOT_OCUPADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devOcupado));
                return;

            case ROBOT_APAGADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devApagado));
                return;
        }
    }

    private boolean verificarBateria(int bateria) {
        if(bateria >= BATERIA_NIVEL_ALTO) {
            mensajeInfoBateria = "";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            //infoBateria.setBackgroundResource(R.color.colorBackground);
            infoBateria.setVisibility(View.GONE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoBateria = "Batería moderada: será necesario recargar pronto.";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            infoBateria.setBackgroundResource(R.drawable.recuadro_sugerencia);
            infoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_BAJO) {
            //Advertencia
            mensajeInfoBateria = "Batería baja: se recomienda recargar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_alert_24);
            infoBateria.setBackgroundResource(R.drawable.recuadro_advertencia);
            infoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria == BATERIA_PROBLEMATICA) {
            mensajeInfoBateria = "Se recomienda reemplazar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            infoBateria.setBackgroundResource(R.drawable.recuadro_problemas);
            infoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        //Alerta
        mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
        imagenBateria.setImageResource(R.drawable.battery_alert_24);
        infoBateria.setBackgroundResource(R.drawable.recuadro_alerta);
        infoBateria.setVisibility(View.VISIBLE);
        return false;
    }

    private boolean verificarNivelQuimico(int nivelQuimico) {
        if(nivelQuimico >= QUIMICO_NIVEL_ALTO) {
            mensajeInfoNivelQuimico = "";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            //imagenQuimico.setPadding(0, 0, 0, 0);
            //infoNivelQuimico.setBackgroundResource(R.color.colorBackground);
            infoNivelQuimico.setVisibility(View.GONE);
            return true;
        }
        if(nivelQuimico >= QUIMICO_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoNivelQuimico = "Nivel de químico moderado: será necesario recargar pronto.";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            //imagenQuimico.setPadding(0, 0, 0, 0);
            infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_advertencia);
            infoNivelQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        if(nivelQuimico == QUIMICO_PROBLEMATICO) {
            mensajeInfoNivelQuimico = "Se recomienda verificar el depósito del químico.";
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
            //imagenQuimico.setPadding(2, 2, 2, 2);
            infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_problemas);
            infoNivelQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        // Nivel de químico bajo
        mensajeInfoNivelQuimico = "Nivel de químico bajo: es necesario recargar.";
        imagenQuimico.setImageResource(R.drawable.ic_science_alert);
        //imagenQuimico.setPadding(2, 2, 2, 2);
        infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_alerta);
        infoNivelQuimico.setVisibility(View.VISIBLE);
        return false;
    }


    public void configurarAdapterListaQuimicos(){
        adapterListaQuimicos = new ArrayAdapter<>(getContext(), R.layout.list_item, robot.getQuimicosDisponibles());
        autoCompleteTextView.setAdapter(adapterListaQuimicos);
    }

    public void configurarAdapterListaCantidadPorArea(){
        adapterListaCantidadArea= new ArrayAdapter<>(getContext(), R.layout.list_item,
                getResources().getStringArray(R.array.cantidad_quimico_por_area));
        autoCompleteTextView2.setAdapter(adapterListaCantidadArea);
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            robot = dataSnapshot.child(Integer.toString(robot.getRobotId())).getValue(Robot.class);
            determinarEstadoRobot(robot);

            // if cuestionable
            if(cantQuimicosAnterior != robot.getQuimicosDisponibles().size()
                    && (isFumigandoAnterior == robot.isFumigando() || robot.isFumigando() == false))
                configurarAdapterListaQuimicos();

            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void iniciarFumigacion(){
        fumigacion = new Fumigacion();
        cronometro.setBase(SystemClock.elapsedRealtime());
        listaQuimicos.setEnabled(false);
        listaCantidadArea.setEnabled(false);
        fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis()));
        cronometro.start();
    }

    public void detenerFumigacion(){
        cronometro.stop();
        listaQuimicos.setEnabled(true);
        listaCantidadArea.setEnabled(true);

        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));
        fumigacion.setQuimicoUtilizado(autoCompleteTextView.getText().toString());
        fumigacion.setCantidadQuimicoPorArea(autoCompleteTextView2.getText().toString());
        updateFumigacion(fumigacion);

        configurarAdapterListaQuimicos(); //actualiza por si hubo un cambio en la lista de químicos
    }

    public void updateRobot(Robot robot) {
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("fumigando", robot.isFumigando());
        childUpdates.put("cantidadQuimicoPorArea", robot.getCantidadQuimicoPorArea());
        referenceRobot.child(Integer.toString(robot.getRobotId())).updateChildren(childUpdates);
    }

    public void updateFumigacion(Fumigacion fumigacion){
        //Necesita hacer una task porque primero consulta la cantidad y necesita esperar a que termine
        Task<DataSnapshot> task = referenceFumigacion.get();

        task.addOnCompleteListener(task1 -> {
            int cant;

            if (task1.isSuccessful()) {
                DataSnapshot snapshot = task1.getResult();
                cant = (int) snapshot.getChildrenCount();

                fumigacion.setFumigacionId("f" + (cant + 1));
                referenceFumigacion.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMap());
            }
        });
    }
}
