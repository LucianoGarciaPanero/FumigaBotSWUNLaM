package com.example.fumigabot.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.fumigabot.NuevaFumigacionActivity;
import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class InicioFragment extends Fragment {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceRobot;
    private DatabaseReference referenceFumigacion;
    private MaterialCardView cardEstadoBateria;
    private MaterialCardView cardEstadoQuimico;
    private ImageView imgEstadoBateria;
    private ImageView imgEstadoQuimico;
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
    private FloatingActionButton fab;
    private Chronometer cronometro;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;
    private Robot robot;
    private Fumigacion fumigacion;
    private Button detenerFumigacion;
    private ActivityResultLauncher<Intent> activityResultLauncher;

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

        //referencia de las fumigaciones para empezar a guardarlas
        referenceFumigacion = firebaseDatabase
            .getReference("fumigaciones_historial/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceFumigacion.keepSynced(true);

        //Register para tomar los datos del fragmento de la nueva fumigación
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK && result.getData() != null){
                            //El callback se ejecuta cuando se cierre la activity de Nueva Fumigación
                            Bundle bundle = result.getData().getExtras();
                            fumigacion = (Fumigacion)bundle.getSerializable("fumigacion_nueva");
                            iniciarFumigacion();
                        }
                    }
                });
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
        cardEstadoBateria = vista.findViewById(R.id.cardEstadoBateria);
        cardEstadoQuimico = vista.findViewById(R.id.cardEstadoQuimico);
        imgEstadoBateria = vista.findViewById(R.id.imgEstadoBateria);
        imgEstadoQuimico = vista.findViewById(R.id.imgEstadoQuimico);

        estadoRobot = vista.findViewById(R.id.estadoRobot);
        textEstadoFumigacion = vista.findViewById(R.id.textEstadoFumigacion);
        infoBateria = vista.findViewById(R.id.infoBateria);
        textBateria = vista.findViewById(R.id.porcentajeBateria);
        imagenBateria = vista.findViewById(R.id.imagenBateria);
        infoNivelQuimico = vista.findViewById(R.id.infoNivelQuimico);
        textNivelQuimico = vista.findViewById(R.id.porcentajeQuimico);
        imagenQuimico = vista.findViewById(R.id.imagenQuimico);

        fab = vista.findViewById(R.id.fabFumigacion);
        fab.setOnClickListener(fabNuevaFumigacionListener);

        cronometro = vista.findViewById(R.id.Cronometro);

        detenerFumigacion = vista.findViewById(R.id.detenerFumigacion);
        detenerFumigacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detenerFumigacion();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        isFumigandoAnterior = robot.isFumigando(); // cuestionable
        cantQuimicosAnterior = robot.getQuimicosDisponibles().size(); // cuestionable
    }

    @Override
    public void onResume() {
        super.onResume();
        this.onStart();
        determinarEstadoRobot(robot);
    }

    /*private View.OnClickListener btnIniciarFumigacionListener = v -> inicializarAlertDialog();*/

  /*  public void inicializarAlertDialog() {
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
    }*/

    public View.OnClickListener fabNuevaFumigacionListener = v -> nuevaFumigacion();

    public void nuevaFumigacion(){
        //startActivity(new Intent(getContext(), NuevaFumigacionActivity.class).putExtra("robot", robot));
        activityResultLauncher.launch(new Intent(getContext(), NuevaFumigacionActivity.class)
                .putExtra("robot_quimicos", robot.getQuimicosDisponibles())
                .putExtra("robotId", robot.getRobotId()));
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

            if(statusBateria && statusNivelQuimico)
                habilitarFumigacion(true);
            else
                habilitarFumigacion(false);


            if(bateria == -1)
                porcentajeBateria = "";
            else
                porcentajeBateria = bateria + "%";

            if(nivelQuimico == -1)
                porcentajeNivelQuimico = "";
            else
                porcentajeNivelQuimico = nivelQuimico + "%";


            if(robot.isFumigando()) {
                //btnIniciarFumigacion.setText("DETENER FUMIGACIÓN");
                fumigando = View.VISIBLE;
                definirEstadoRobot(ROBOT_OCUPADO, "Fumigando...");
            }
            else {
                //btnIniciarFumigacion.setText("FUMIGAR");
                fumigando = View.INVISIBLE;
                definirEstadoRobot(ROBOT_ENCENDIDO, "Encendido");
            }
        }
        else {
            porcentajeBateria = porcentajeNivelQuimico = "";
            definirEstadoRobot(ROBOT_APAGADO, "Apagado");
            /*mensajeInfoBateria = "Encender el robot para comenzar";
            mensajeInfoNivelQuimico = "";*/
            //infoBateria.setBackgroundColor(R.attr.backgroundColor);
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            //infoNivelQuimico.setBackgroundColor(R.attr.backgroundColor);
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
            //habilitarFumigacion(false); //está apagado
        }

        infoBateria.setText(mensajeInfoBateria);
        textBateria.setText(porcentajeBateria);
        infoNivelQuimico.setText(mensajeInfoNivelQuimico);
        textNivelQuimico.setText(porcentajeNivelQuimico);
        textEstadoFumigacion.setVisibility(fumigando);
        cronometro.setVisibility(fumigando);
    }

    private void habilitarFumigacion(boolean valor){
        /*btnIniciarFumigacion.setEnabled(valor);
        listaQuimicos.setEnabled(valor);
        listaCantidadArea.setEnabled(valor);*/
        fab.setEnabled(valor);
    }

    private void definirEstadoRobot(int estado, String desc){
        estadoRobot.setText(desc);

        switch (estado){
            case ROBOT_ENCENDIDO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devEncendido));
                habilitarFumigacion(true);
                return;

            case ROBOT_OCUPADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devOcupado));
                habilitarFumigacion(false);
                return;

            case ROBOT_APAGADO:
                estadoRobot.setTextColor(getResources().getColor(R.color.devApagado));
                habilitarFumigacion(false);
                return;
        }
    }

    @SuppressLint("NewApi")
    private boolean verificarBateria(int bateria) {
        if(bateria >= BATERIA_NIVEL_ALTO) {
            mensajeInfoBateria = "";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            //infoBateria.setBackgroundResource(R.color.colorBackground);
            cardEstadoBateria.setVisibility(View.GONE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoBateria = "Batería moderada: será necesario recargar pronto.";
            imagenBateria.setImageResource(R.drawable.battery_full_24);
            //infoBateria.setBackgroundResource(R.drawable.recuadro_sugerencia);
            imgEstadoBateria.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoSugerencia);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);
            //getResources().getDrawable(R.drawable.round_warning_amber_24).setTint(getResources().getColor(R.color.fondoSugerencia));
            cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria >= BATERIA_NIVEL_BAJO) {
            //Advertencia
            mensajeInfoBateria = "Batería baja: se recomienda recargar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_alert_24);
            //infoBateria.setBackgroundResource(R.drawable.recuadro_advertencia);
            imgEstadoBateria.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoAdvertencia);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);
            cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        if(bateria == BATERIA_PROBLEMATICA) {
            mensajeInfoBateria = "Se recomienda reemplazar la batería.";
            imagenBateria.setImageResource(R.drawable.battery_unknown_24);
            //infoBateria.setBackgroundResource(R.drawable.recuadro_problemas);
            imgEstadoBateria.setImageResource(R.drawable.round_error_outline_24);
            int color = getResources().getColor(R.color.fondoAvisoBateria);
            imgEstadoBateria.getDrawable().setTint(color);
            infoBateria.setTextColor(color);
            cardEstadoBateria.setVisibility(View.VISIBLE);
            return true;
        }
        //Alerta
        mensajeInfoBateria = "Batería muy baja: el dispositivo se apagará pronto.";
        imagenBateria.setImageResource(R.drawable.battery_alert_24);
        //infoBateria.setBackgroundResource(R.drawable.recuadro_alerta);
        imgEstadoBateria.setImageResource(R.drawable.round_error_outline_24);
        int color = getResources().getColor(R.color.fondoAlerta);
        imgEstadoBateria.getDrawable().setTint(color);
        infoBateria.setTextColor(color);
        cardEstadoBateria.setVisibility(View.VISIBLE);
        return false;
    }

    @SuppressLint("NewApi")
    private boolean verificarNivelQuimico(int nivelQuimico) {
        if(nivelQuimico >= QUIMICO_NIVEL_ALTO) {
            mensajeInfoNivelQuimico = "";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            //imagenQuimico.setPadding(0, 0, 0, 0);
            //infoNivelQuimico.setBackgroundResource(R.color.colorBackground);
            cardEstadoQuimico.setVisibility(View.GONE);
            return true;
        }
        if(nivelQuimico >= QUIMICO_NIVEL_MODERADO) {
            //Sugerencia
            mensajeInfoNivelQuimico = "Nivel de químico moderado: será necesario recargar pronto.";
            imagenQuimico.setImageResource(R.drawable.ic_science);
            //imagenQuimico.setPadding(0, 0, 0, 0);
            //infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_advertencia);
            imgEstadoQuimico.setImageResource(R.drawable.round_warning_amber_24);
            int color = getResources().getColor(R.color.fondoSugerencia);
            imgEstadoQuimico.getDrawable().setTint(color);
            infoNivelQuimico.setTextColor(color);
            cardEstadoQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        if(nivelQuimico == QUIMICO_PROBLEMATICO) {
            mensajeInfoNivelQuimico = "Se recomienda verificar el depósito del químico.";
            imagenQuimico.setImageResource(R.drawable.ic_science_unknown);
            //imagenQuimico.setPadding(2, 2, 2, 2);
            //infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_problemas);
            imgEstadoQuimico.setImageResource(R.drawable.round_error_outline_24);
            int color = getResources().getColor(R.color.fondoAvisoBateria);
            imgEstadoQuimico.getDrawable().setTint(color);
            infoNivelQuimico.setTextColor(color);
            cardEstadoQuimico.setVisibility(View.VISIBLE);
            return true;
        }
        // Nivel de químico bajo
        mensajeInfoNivelQuimico = "Nivel de químico bajo: es necesario recargar.";
        imagenQuimico.setImageResource(R.drawable.ic_science_alert);
        //imagenQuimico.setPadding(2, 2, 2, 2);
        //infoNivelQuimico.setBackgroundResource(R.drawable.recuadro_alerta);
        imgEstadoQuimico.setImageResource(R.drawable.round_error_outline_24);
        int color = getResources().getColor(R.color.fondoAlerta);
        imgEstadoQuimico.getDrawable().setTint(color);
        infoNivelQuimico.setTextColor(color);
        cardEstadoQuimico.setVisibility(View.VISIBLE);
        return false;
    }


    private ValueEventListener robotValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            robot = dataSnapshot.child(Integer.toString(robot.getRobotId())).getValue(Robot.class);
            determinarEstadoRobot(robot);
/*
            // if cuestionable
            if(cantQuimicosAnterior != robot.getQuimicosDisponibles().size()
                    && (isFumigandoAnterior == robot.isFumigando() || robot.isFumigando() == false))
                //configurarAdapterListaQuimicos();
*/
            return;
        }

        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void iniciarFumigacion(){
        robot.setFumigando(true);
        robot.convertirCantidadQuimicoPorArea(fumigacion.getCantidadQuimicoPorArea());
        cronometro.setBase(SystemClock.elapsedRealtime());
        //Actualizamos la hora a ahora
        fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis()));
        cronometro.start();
        updateRobot(robot);
    }

    public void detenerFumigacion(){
        cronometro.stop();
        fumigacion.setTimestampFin(Long.toString(System.currentTimeMillis()));
        robot.setFumigando(false);
        updateRobot(robot);
        updateFumigacion(fumigacion);
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

                fumigacion.setFumigacionId("fh" + (cant + 1));
                referenceFumigacion.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMap());
            }
        });
    }
}
