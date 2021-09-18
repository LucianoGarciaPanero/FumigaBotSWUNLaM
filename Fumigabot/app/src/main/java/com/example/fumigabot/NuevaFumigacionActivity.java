package com.example.fumigabot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.aceinteract.android.stepper.StepperNavListener;
import com.aceinteract.android.stepper.StepperNavigationView;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.steps.Step1Fragment;
import com.example.fumigabot.steps.Step2Fragment;
import com.example.fumigabot.steps.Step3Fragment;
import com.example.fumigabot.steps.Step4Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import java.util.ArrayList;

public class NuevaFumigacionActivity extends AppCompatActivity implements StepperNavListener {

    private Fumigacion fumigacion;
    private ArrayList<String> quimicosDisponibles;
    private FragmentManager fragmentManager;
    private StepperNavigationView stepper;
    private MaterialButton anterior;
    private MaterialButton siguiente;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;

    private static final String STEP_1 = "step_1";
    private static final String STEP_2 = "step_2";
    private static final String STEP_3 = "step_3";
    private static final String STEP_4 = "step_4";

    private ItemViewModel viewModelNuevaFumigacion;

    public NuevaFumigacionActivity() {
        super(R.layout.activity_nueva_fumigacion);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_nueva_fumigacion);
        super.onCreate(savedInstanceState);

        //Obtenemos el manager y creamos todos los fragments
        fragmentManager = getSupportFragmentManager();

        //Obtenemos los químicos disponibles para el robot
        quimicosDisponibles = (ArrayList<String>) getIntent().getSerializableExtra("robot_quimicos");

        fumigacion = new Fumigacion();

        stepper = findViewById(R.id.stepper);
        anterior = findViewById(R.id.anterior);
        anterior.setOnClickListener(anteriorListener);
        siguiente = findViewById(R.id.siguiente);
        siguiente.setOnClickListener(siguienteListener);
        //stepper.setupWithNavController(NavHostFragment.findNavController(fragmentManager.findFragmentById(R.id.frame_stepper)));
        stepper.setStepperNavListener(this);

        if(fragmentManager.getFragments().size() == 0) {
            //El "intent" entre fragments
            Bundle bundleQuimicos = new Bundle();
            bundleQuimicos.putSerializable("quimicos",quimicosDisponibles);

            try {
                fragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.frame_stepper, Step1Fragment.class, bundleQuimicos, STEP_1)
                        .add(R.id.frame_stepper, Step2Fragment.class, null, STEP_2)
                        .add(R.id.frame_stepper, Step3Fragment.class, null, STEP_3)
                        .add(R.id.frame_stepper, Step4Fragment.class, null, STEP_4)
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
        //Selección del químico
        viewModelNuevaFumigacion = new ViewModelProvider(this).get(ItemViewModel.class);
        viewModelNuevaFumigacion.getQuimicoSeleccionado().observe(this, item -> {
            quimicoSeleccionado(item);
        });

        //Cantidad seleccionada
        viewModelNuevaFumigacion.getCantidadSeleccionada().observe(this, item -> {
            cantidadSeleccionada(item);
        });
    }

    private void quimicoSeleccionado(ClipData.Item item){
        boolean habilitar = false;

        if(item != null) {
            fumigacion.setQuimicoUtilizado(item.getText().toString());
            habilitar = true;
        }
        siguiente.setEnabled(habilitar);
    }

    private void cantidadSeleccionada(ClipData.Item item){
        boolean habilitar = false;

        if(item != null) {
            fumigacion.setCantidadQuimicoPorArea(item.getText().toString());
            habilitar = true;
        }
        siguiente.setEnabled(habilitar);
    }

    @Override
    public void onCompleted() {
        //Toast.makeText(this, "Stepper completed", Toast.LENGTH_SHORT).show();

        //A este punto, ya tenemos creado el objeto Fumigacion, que es la fumigacion que acabamos de crear
        //Es acá donde tenemos que mostrar el Alert Dialog para tener confirmación
        inicializarAlertDialog();
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
        //Seteamos el tiempo de comienzo
        fumigacion.setTimestampInicio(Long.toString(System.currentTimeMillis()));
        //La creacion de la fumigacion se hace cuando se obtiene el tiempo final:
        //tenemos que pasarle la fumigacion al home/main host:
        setResult(RESULT_OK, new Intent().putExtra("fumigacion_nueva", fumigacion));
        finish();
        /*todo:
           cuando se vuelve al home, hay que hacer un refresh del status y arrancar el timer*/
    }

    @Override
    public void onBackPressed() {
        //Cancelar el envío de la nueva fumigación si se presiona "atrás"
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onStepChanged(int i) {
        switch(i){
            case 0:
                //Seleccionar el químico
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_1)).commitNow();

                anterior.setEnabled(false);
                return;

            case 1:
                //Seleccionar cantidad por área
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_2)).commitNow();

                anterior.setEnabled(true);
                siguiente.setEnabled(false);
                return;

            case 2:
                //Seleccionar horario
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_4))
                        .show(fragmentManager.findFragmentByTag(STEP_3)).commitNow();
                reiniciarBotonSiguiente();
                return;

            case 3:
                //Confirmación de los parámetros
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(STEP_1))
                        .hide(fragmentManager.findFragmentByTag(STEP_2))
                        .hide(fragmentManager.findFragmentByTag(STEP_3))
                        .show(fragmentManager.findFragmentByTag(STEP_4)).commitNow();
                configurarBotonFinal();
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
