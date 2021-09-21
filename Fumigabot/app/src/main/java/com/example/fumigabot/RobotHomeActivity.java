package com.example.fumigabot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.fumigabot.firebase.Robot;
import com.example.fumigabot.home.HistorialFragment;
import com.example.fumigabot.home.InicioFragment;
import com.example.fumigabot.home.ProgramadasFragment;
import com.example.fumigabot.home.QuimicosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RobotHomeActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    public Robot robot;
    private FragmentManager fragmentManager;
    private BottomNavigationView bottomNav;
    private static final String INICIO_FRAGMENTO = "inicio_fragmento";
    private static final String HISTORIAL_FRAGMENTO = "historial_fragmento";
    private static final String QUIMICOS_FRAGMENTO = "quimicos_fragmento";
    private static final String PROGRAMADAS_FRAGMENTO = "programadas_fragmento";


    public RobotHomeActivity() {
        super(R.layout.activity_robot_home);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_robot_home);
        super.onCreate(savedInstanceState);

        //Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate " + SystemClock.elapsedRealtime());

        //Obtenemos el manager y creamos todos los fragments
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            //Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate - savedInstanceState es NULO " + SystemClock.elapsedRealtime());
            robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");
        }
        //else
            //Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate - savedInstanceState es NO NULO " + SystemClock.elapsedRealtime());


        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("FILTRO", "FRAGMENT ACTIVITY: onStart " + SystemClock.elapsedRealtime());
        if(fragmentManager.getFragments().size() == 0) {
            //Funciona como el intent entre activities, pero acá es entre fragments
            Bundle bundle = new Bundle();
            bundle.putSerializable("RobotVinculado", robot);
            try {
                fragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragment_container_view, InicioFragment.class, bundle, INICIO_FRAGMENTO)
                        .add(R.id.fragment_container_view, HistorialFragment.class, bundle, HISTORIAL_FRAGMENTO)
                        .add(R.id.fragment_container_view, QuimicosFragment.class, bundle, QUIMICOS_FRAGMENTO)
                        .add(R.id.fragment_container_view, ProgramadasFragment.class, bundle, PROGRAMADAS_FRAGMENTO)
                        //.addToBackStack(null)
                        .commitNow();

                //Mostramos solamente el home en un principio
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(PROGRAMADAS_FRAGMENTO))
                        .commitNow();
            } catch (Exception e) {
                //Log.i("FILTRO", "CATCH: " + e.getMessage());
            }
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.pageInicio:
                //Seleccionar el fragmento del inicio
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(PROGRAMADAS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO)).commitNow();
                return true;

            case R.id.pageHistorial:
                //Seleccionar el fragmento del historial
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(PROGRAMADAS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO)).commitNow();
                return true;

            case R.id.pageQuimicos:
                //Seleccionar el fragmento de los químicos
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(PROGRAMADAS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO)).commitNow();
                return true;

            case R.id.pageProgramadas:
                //Seleccionar el fragmento de los químicos
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(PROGRAMADAS_FRAGMENTO)).commitNow();
                return true;
        }
        return false;
    }
}
