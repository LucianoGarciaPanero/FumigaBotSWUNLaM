package com.example.fumigabot.home;


import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
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
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * A simple {@link Fragment} subclass.
 */
public class InicioFragment extends Fragment {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceFumigacion;
    private DatabaseReference referenceQuimicosDisponibles;
    private TextView textActividadRobot;
    private TextView textBateria;
    private TextView infoBateria;
    private TextView textNivelQuimico;
    private TextView infoNivelQuimico;
    private TextView textQuimicosDisponibles;
    private String mensajeInfoBateria;
    private String mensajeInfoNivelQuimico;
    private Button btnIniciarFumigacion;
    private Chronometer cronometro;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    private Robot robot;
    private Fumigacion fumigacion;
    private Spinner listaQuimicos;
    private ArrayAdapter<String> adapterListaQuimicos;
    private final int BATERIA_NIVEL_ALTO = 40;
    private final int BATERIA_NIVEL_MODERADO = 15;
    private final int BATERIA_NIVEL_BAJO = 5;
    private final int BATERIA_PROBLEMATICA = -1;
    private final int QUIMICO_NIVEL_ALTO = 40;
    private final int QUIMICO_NIVEL_MODERADO = 20;
    private final int QUIMICO_PROBLEMATICO = -1;
    private boolean isFumigandoAnterior;
    private int cantQuimicosAnterior;


    public InicioFragment() {
        // Required empty public constructor
        super(R.layout.fragment_inicio);
    }

    /*public static InicioFragment newInstance(Robot robot) {
        InicioFragment inicioFragment = new InicioFragment();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable("robot", robot);
        inicioFragment.setArguments(argumentos);
        return inicioFragment;
    }*/

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("FILTRO", "INICIO FRAGMENT: onCreate " + SystemClock.elapsedRealtime());

        //TransitionInflater inflater = TransitionInflater.from(requireContext());

        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());


        //-------------- LO QUE ESTABA EN LA ACTIVITY ORIGINAL ---------------

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

        textActividadRobot = getView().findViewById(R.id.textActividadRobot);
        textBateria = getView().findViewById(R.id.textBateria);
        infoBateria = getView().findViewById(R.id.infoBateria);
        textNivelQuimico = getView().findViewById(R.id.textNivelQuimico);
        infoNivelQuimico = getView().findViewById(R.id.infoNivelQuimico);

        textQuimicosDisponibles = getView().findViewById(R.id.textQuimicosDisponibles);
        listaQuimicos = getView().findViewById(R.id.listaQuimicos);


        btnIniciarFumigacion = getView().findViewById(R.id.btnIniciarFumigacion);
        btnIniciarFumigacion.setOnClickListener(btnIniciarFumigacionListener);

        cronometro = getView().findViewById(R.id.Cronometro);
    }

    @Override
    public void onStart() {
        super.onStart();



        //robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");
        isFumigandoAnterior = robot.isFumigando(); // cuestionable
        cantQuimicosAnterior = robot.getQuimicosDisponibles().size(); // cuestionable

        //referencia de las fumigaciones para empezar a guardarlas
        referenceFumigacion = firebaseDatabase.getReference("fumigaciones/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceFumigacion.keepSynced(true);

        configurarAdapterListaQuimicos();

    }

     private View.OnClickListener btnIniciarFumigacionListener = v -> inicializarAlertDialog();


    public void inicializarAlertDialog() {
        builder = new AlertDialog.Builder(getContext(), R.style.alertDialogStyle);

        String titleAlertDialog;
        if(!robot.isFumigando())
            titleAlertDialog = "iniciar una ";
        else
            titleAlertDialog = "finalizar la ";
        builder.setMessage("¿Seguro querés " + titleAlertDialog + "fumigación?");

        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
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

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    public void determinarEstadoRobot(Robot robot){
        String estado;
        String porcentajeBateria;
        String porcentajeNivelQuimico = "";
        boolean statusBateria = false;
        boolean statusNivelQuimico = false;
        int fumigando = View.INVISIBLE;
        int bateria = robot.getBateria();
        int nivelQuimico = robot.getNivelQuimico();

        if(robot.isEncendido()) {
            statusBateria = verificarBateria(bateria);
            statusNivelQuimico = verificarNivelQuimico(nivelQuimico);

            if(bateria == -1)
                porcentajeBateria = "Batería: con problemas"; //\n\n\nENCENDIDO\n\n";
            else
                porcentajeBateria = "Batería: " + bateria + "%"; //\n\n\nENCENDIDO\n\n";

            if(nivelQuimico == -1)
                porcentajeNivelQuimico = "Depósito de químico con problemas";
            else
                porcentajeNivelQuimico = "Nivel químico: " + nivelQuimico + "%";

            textQuimicosDisponibles.setVisibility(View.VISIBLE);
            listaQuimicos.setVisibility(View.VISIBLE);

            if(robot.isFumigando()) {
                estado = "FUMIGANDO...";
                btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
                fumigando = View.VISIBLE;
            }
            else {
                estado = "ESPERANDO ÓRDENES...";
                btnIniciarFumigacion.setText("FUMIGAR");
                fumigando = View.INVISIBLE;
            }
        }
        else {
            porcentajeBateria = "APAGADO\n\n\n";
            mensajeInfoBateria = "";
            mensajeInfoNivelQuimico = "";
            infoBateria.setBackgroundResource(R.color.colorBackground);
            infoNivelQuimico.setBackgroundResource(R.color.colorBackground);
            textQuimicosDisponibles.setVisibility(View.INVISIBLE);
            listaQuimicos.setVisibility(View.INVISIBLE);

            estado = "Encender el dispositivo para comenzar";
            btnIniciarFumigacion.setText("FUMIGAR");
        }

        infoBateria.setText(mensajeInfoBateria);
        textBateria.setText(porcentajeBateria);
        infoNivelQuimico.setText(mensajeInfoNivelQuimico);
        textNivelQuimico.setText(porcentajeNivelQuimico);
        textActividadRobot.setText(estado);

        if(statusBateria && statusNivelQuimico)
            btnIniciarFumigacion.setEnabled(true);
        else
            btnIniciarFumigacion.setEnabled(false);

        cronometro.setVisibility(fumigando);
    }

    private boolean verificarBateria(int bateria) {
        if(bateria >= BATERIA_NIVEL_ALTO) {
            mensajeInfoBateria = "";
            infoBateria.setBackgroundResource(R.color.colorBackground);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoBateria = "Batería moderada: será necesario recargar pronto.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_sugerencia);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_BAJO) {
            //Advertencia
            mensajeInfoBateria = "Batería baja: se recomienda recargar la batería.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_advertencia);
            return true;
        }
        if(bateria == BATERIA_PROBLEMATICA) {
            mensajeInfoBateria = "Se recomienda reemplazar la batería.";
            infoBateria.setBackgroundResource(R.drawable.recuadro_problemas);
            return true;
        }
        //Alerta
        mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
        infoBateria.setBackgroundResource(R.drawable.recuadro_alerta);
        return false;
    }

    private boolean verificarNivelQuimico(int nivelQuimico) {
        if(nivelQuimico >= QUIMICO_NIVEL_ALTO) {
            mensajeInfoNivelQuimico = "";
            infoNivelQuimico.setBackgroundResource(R.color.colorBackground);
            return true;
        }
        if(nivelQuimico >= QUIMICO_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoNivelQuimico = "Nivel de químico moderado: será necesario recargar pronto.";
            infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_advertencia);
            return true;
        }
        if(nivelQuimico == QUIMICO_PROBLEMATICO) {
            mensajeInfoNivelQuimico = "Se recomienda verificar el depósito del químico.";
            infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_problemas);
            return true;
        }
        // Nivel de químico bajo
        mensajeInfoNivelQuimico = "Nivel de químico bajo: es necesario recargar.";
        infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_alerta);
        return false;
    }

    public void configurarAdapterListaQuimicos(){
        adapterListaQuimicos = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                robot.getQuimicosDisponibles());
        adapterListaQuimicos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listaQuimicos.setAdapter(adapterListaQuimicos);
    }

    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            robot = dataSnapshot.child(Integer.toString(robot.getRobotId())).getValue(Robot.class);
            determinarEstadoRobot(robot);

            // if cuestionable
            if(isFumigandoAnterior == robot.isFumigando()
                    && cantQuimicosAnterior != robot.getQuimicosDisponibles().size())
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
        //cronometro.setBase(System.currentTimeMillis());//Clock.elapsedRealtime());
        listaQuimicos.setEnabled(false);
        fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis())); //Long.toString(cronometro.getBase()));
        cronometro.start();
    }

    public void detenerFumigacion(){
        cronometro.stop();
        listaQuimicos.setEnabled(true);

        //long tiempoTranscurrido = SystemClock.elapsedRealtime() - cronometro.getBase();
        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));//(Long.toString(tiempoTranscurrido));
        fumigacion.setQuimicoUtilizado(listaQuimicos.getSelectedItem().toString());
        updateFumigacion(fumigacion);

        configurarAdapterListaQuimicos(); //actualiza por si hubo un cambio en la lista de químicos
    }

    public void updateRobot(Robot robot) {
        referenceRobot.child(Integer.toString(robot.getRobotId()))
                .child("fumigando")
                .setValue(robot.isFumigando());
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
