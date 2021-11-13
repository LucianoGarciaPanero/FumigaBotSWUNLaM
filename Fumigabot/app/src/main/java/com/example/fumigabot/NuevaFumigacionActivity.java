package com.example.fumigabot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.aceinteract.android.stepper.StepperNavListener;
import com.aceinteract.android.stepper.StepperNavigationView;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.example.fumigabot.steps.Step2Fragment;
import com.example.fumigabot.steps.Step3Fragment;
import com.example.fumigabot.steps.Step1Fragment;
import com.example.fumigabot.steps.Step4Fragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NuevaFumigacionActivity extends AppCompatActivity implements StepperNavListener, LifecycleOwner {

    private Fumigacion fumigacion;
    private Robot robot;
    private ArrayList<String> quimicosDisponibles;
    private FragmentManager fragmentManager;
    private StepperNavigationView stepper;
    private MaterialButton anterior;
    private MaterialButton siguiente;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;
    private ItemViewModel viewModelNuevaFumigacion;
    private FirebaseFunctions firebaseFunctions;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference referenceProgramadas;
    private boolean habilitarStepQuimico = false;
    private boolean habilitarStepCantidad = false;
    private boolean habilitarStepFecha = false;

    private static final String STEP_1 = "step_1";
    private static final String STEP_2 = "step_2";
    private static final String STEP_3 = "step_3";
    private static final String STEP_4 = "step_4";


    public NuevaFumigacionActivity() {
        super(R.layout.activity_nueva_fumigacion);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_nueva_fumigacion);
        super.onCreate(savedInstanceState);

        //Obtenemos el Id y los químicos disponibles para el robot
        quimicosDisponibles = (ArrayList<String>) getIntent().getSerializableExtra("robot_quimicos");
        robot = (Robot)getIntent().getSerializableExtra("robot");
        if(robot.isFumigando() == false){
            habilitarStepFecha = true;
        }

        //Instancia de las Functions en Firebase
        firebaseFunctions = MyFirebase.getFunctionsInstance();
        //Instancia de la BD en Firebase
        firebaseDatabase = MyFirebase.getDatabaseInstance();
        //referencia de las fumigaciones programadas para empezar a guardarlas
        referenceProgramadas = firebaseDatabase
                .getReference("fumigaciones_programadas/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        referenceProgramadas.keepSynced(true);

        inicializarFragmentos();


        fumigacion = new Fumigacion();
        stepper = findViewById(R.id.stepper);
        anterior = findViewById(R.id.anterior);
        anterior.setOnClickListener(anteriorListener);
        siguiente = findViewById(R.id.siguiente);
        siguiente.setOnClickListener(siguienteListener);
        //stepper.setupWithNavController(NavHostFragment.findNavController(fragmentManager.findFragmentById(R.id.frame_stepper)));
        stepper.setStepperNavListener(this);
    }

    private void inicializarFragmentos(){
        //Obtenemos el manager y creamos todos los fragments
        fragmentManager = getSupportFragmentManager();

        if(fragmentManager.getFragments().size() == 0) {
            //El "intent" entre fragments
            Bundle bundleQuimicos = new Bundle();
            bundleQuimicos.putSerializable("quimicos", quimicosDisponibles);
            bundleQuimicos.putString("quimicoRobot", robot.getUltimoQuimico());

            Bundle bundleFumigando = new Bundle();
            bundleFumigando.putBoolean("fumigando", robot.isFumigando());

            try {
                fragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.frame_stepper, Step1Fragment.class, bundleFumigando, STEP_1)
                        .add(R.id.frame_stepper, Step2Fragment.class, bundleQuimicos, STEP_2)
                        .add(R.id.frame_stepper, Step3Fragment.class, null, STEP_3)
                        .add(R.id.frame_stepper, Step4Fragment.class, null, STEP_4)
                        //.addToBackStack(null)
                        .commitNow();

                //Mostramos solamente el primero en principio
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .hide(fragmentManager.findFragmentByTag(STEP_4)).commitNow();
            } catch (Exception e) {
            }
            configurarViewModels();
        }
    }

    private void configurarViewModels(){
        viewModelNuevaFumigacion = new ViewModelProvider(this).get(ItemViewModel.class);
        //Selección del químico
        viewModelNuevaFumigacion.getQuimicoSeleccionado().observe(this, item -> {
            quimicoSeleccionado(item);
        });

        //Selección de la cantidad
        viewModelNuevaFumigacion.getCantidadSeleccionada().observe(this, item -> {
            cantidadSeleccionada(item);
        });

        //Selección del horario
        viewModelNuevaFumigacion.getHorarioSeleccionado().observe(this, item->{
            horarioSeleccionado(item);
        });

        //Se repite diaramente (que por ahora es el "es recurrente")
        viewModelNuevaFumigacion.getRepetirDiariamente().observe(this, item->{
             repetirDiariamente(item);
        });
    }

    private void quimicoSeleccionado(String item){
        if(item != null) {
            fumigacion.setQuimicoUtilizado(item);
            habilitarStepQuimico = true;
        }
        siguiente.setEnabled(habilitarStepQuimico);
    }

    private void cantidadSeleccionada(ClipData.Item item){
        if(item != null) {
            fumigacion.setCantidadQuimicoPorArea(item.getText().toString());
            habilitarStepCantidad = true;
        }
        siguiente.setEnabled(habilitarStepCantidad);
    }

    private void horarioSeleccionado(Date fecha){
        boolean habilitar = false;

        if(fecha != null) {
            fumigacion.setTimestampInicio(Long.toString(fecha.getTime()));// item.getText().toString());
            //fumigacion.setProgramada(true);
            habilitar = true;
        }
        siguiente.setEnabled(habilitar);
    }

    private void repetirDiariamente(Boolean repetir){
        //boolean habilitar = false;

        if(repetir != null) {
            fumigacion.setRecurrente(repetir);
            //habilitar = true;
        }
        //siguiente.setEnabled(habilitar);
    }

    @Override
    public void onCompleted() {
        //A este punto, ya tenemos creado el objeto Fumigacion
        //Si es instantánea, la hacemos ahora
        if(viewModelNuevaFumigacion.isInstantanea().getValue()) {
            inicializarAlertDialog();
        }
        else
            crearProgramada();
    }

    public void inicializarAlertDialog() {
        builder = new MaterialAlertDialogBuilder(this);

        builder.setMessage("¿Seguro querés iniciar una fumigación?");

        builder.setPositiveButton("Iniciar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Iniciamos la fumigación en FB
                iniciarFumigacion();
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

    public void iniciarFumigacion(){
        //podemos consultar acá a Functions que verifique la fumigación
        //pasamos lo que queremos a la funcion callable
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("robotId", robot.getRobotId());
        hashMap.put("fumigacionId", "instantanea");
        hashMap.put("tsInicio", fumigacion.getTimestampInicio());
        hashMap.put("quimicoUtilizado", fumigacion.getQuimicoUtilizado());

        validarFumigacionInstantanea(hashMap).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                String resultado = "";

                if(task.isComplete()) {

                    if (task.isSuccessful()) {
                        resultado = task.getResult();
                        //Si todo sale bien, tomamos el resultado de si podemos o no iniciar la fumigación ahora
                        if (resultado.equalsIgnoreCase("Ok")) {
                            resultado = "Se inició la fumigación";
                            //tenemos que pasarle la fumigacion al home/main host:
                            setResult(RESULT_OK, new Intent().putExtra("fumigacion_nueva", fumigacion));
                            finish();
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Object details = ffe.getDetails();
                            Log.i("test", "Firebase Functions Exception: Code " + code);

                        }
                        resultado = e.getMessage();
                        Log.i("test", "task no es successful, resultado: " + resultado);
                    }
                    //cambiar la forma en que informa al user que no lo hizo
                    runOnUiThread(Toast.makeText(getApplicationContext(), resultado, Toast.LENGTH_SHORT)::show);
                }
            }
        });
    }

    private Task<String> validarFumigacionInstantanea(Map<String, Object> fumigacion){
        //invocamos a la Function
        return firebaseFunctions.getHttpsCallable("evaluarInstantanea")
                .call(fumigacion)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        //esta continuacion se ejecuta en caso de éxito o falla, pero si la Task
                        //falla, entonces gtResult() va a arrojar una excepción la cual se va a propagar
                        String resultado = (String) task.getResult().getData();
                        Log.i("test", "Task retorna resultado: " + resultado);
                        return resultado;
                    }
                });
    }

    public void crearProgramada(){
        fumigacion.setProgramada(true);
        //Necesita hacer una task porque primero consulta la cantidad y necesita esperar a que termine
        Task<DataSnapshot> task = referenceProgramadas.get();

        task.addOnCompleteListener(task1 -> {
            int cant;

            if (task1.isSuccessful()) {
                DataSnapshot snapshot = task1.getResult();
                cant = (int) snapshot.getChildrenCount();

                fumigacion.setFumigacionId("fp" + (cant + 1));
                referenceProgramadas.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMapProgramada());
            }
        });

        Toast.makeText(this, "Se guardó programada", Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onStepChanged(int i) {
        switch(i){
            case 0:
                //Seleccionar fecha y hora
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_1)).commitNow();

                anterior.setEnabled(false);
                siguiente.setEnabled(habilitarStepFecha);
                return;

            case 1:
                //Seleccionar químico
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_2)).commitNow();

                anterior.setEnabled(true);
                siguiente.setEnabled(habilitarStepQuimico);
                return;

            case 2:
                //Seleccionar cantidad de químico por área
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_3)).commitNow();
                reiniciarBotonSiguiente();
                siguiente.setEnabled(habilitarStepCantidad);
                return;

            case 3:
                //Confirmación de los parámetros
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .show(fragmentManager.findFragmentByTag(STEP_4)).commitNow();
                configurarBotonFinal();
                mostrarResumen();
                return;

        }
        return;
    }

    private void reiniciarBotonSiguiente(){
        siguiente.setText("Siguiente");
        siguiente.setIcon(getDrawable(R.drawable.ic_navigate_next_black_24dp));
    }

    private void configurarBotonFinal(){
        siguiente.setText("Finalizar");
        siguiente.setIcon(getDrawable(R.drawable.ic_check_black_24dp));
    }

    private void mostrarResumen(){
        viewModelNuevaFumigacion.setDisponible();
    }

    private View.OnClickListener anteriorListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stepper.goToPreviousStep();
        }
    };

    private View.OnClickListener siguienteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            stepper.goToNextStep();
        }
    };
}
