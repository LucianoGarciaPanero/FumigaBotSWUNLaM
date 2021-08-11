package com.example.fumigabot.home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;

import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Robot;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class FragmentActivity extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private FragmentManager fragmentManager;
    private NavHostFragment navHostFragment;
    private NavController navController;
    private BottomNavigationView bottomNav;

    private static final String INICIO_FRAGMENTO = "inicio_fragmento";
    private static final String HISTORIAL_FRAGMENTO = "historial_fragmento";
    private static final String QUIMICOS_FRAGMENTO = "quimicos_fragmento";

    private InicioFragment inicioFragment;
    private HistorialFragment historialFragment;
    public Robot robot;


    public FragmentActivity() {
        super(R.layout.activity_fragment);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_fragment);
        super.onCreate(savedInstanceState);

        Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate " + SystemClock.elapsedRealtime());

        //Obtenemos el manager y creamos todos los fragments
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate - savedInstanceState es NULO " + SystemClock.elapsedRealtime());
            //Esto es para pasarle info al fragment, tal como un intent entre activities
            //Bundle bundle = new Bundle();
            //bundle.putSerializable("RobotVinculado", getIntent().getSerializableExtra("RobotVinculado"));
            //robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");
            robot = (Robot)getIntent().getSerializableExtra("RobotVinculado");
            /*inicioFragment = InicioFragment.newInstance(robot);
            historialFragment = HistorialFragment.newInstance(robot);*/
        }
        else
            Log.i("FILTRO", "FRAGMENT ACTIVITY: onCreate - savedInstanceState es NO NULO " + SystemClock.elapsedRealtime());



        bottomNav = findViewById(R.id.bottom_nav);
        //bottomNav.setOnItemSelectedListener(bottomNavListener);
        bottomNav.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        /*navHostFragment = (NavHostFragment) fragmentManager.findFragmentById(R.id.fragment_container_view);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("FILTRO", "FRAGMENT ACTIVITY: onStart " + SystemClock.elapsedRealtime());
        if(fragmentManager.getFragments().size() == 0) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("RobotVinculado", robot);
            try {
                fragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        //.add(R.id.fragment_container_view, inicioFragment, INICIO_FRAGMENTO) //, new Fragment(), bundle, INICIO_FRAGMENTO)
                        //.add(R.id.fragment_container_view, historialFragment, HISTORIAL_FRAGMENTO) //HistorialFragment.class, bundle, HISTORIAL_FRAGMENTO)/*
                        .add(R.id.fragment_container_view, InicioFragment.class, bundle, INICIO_FRAGMENTO)
                        .add(R.id.fragment_container_view, HistorialFragment.class, bundle, HISTORIAL_FRAGMENTO)
                        .add(R.id.fragment_container_view, QuimicosFragment.class, bundle, QUIMICOS_FRAGMENTO)
                        //.addToBackStack(null)
                        .commitNow();

                //Mostramos solamente el home en un principio
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO)).commitNow();
            } catch (Exception e) {
                Log.i("FILTRO", "CATCH: " + e.getMessage());
            }
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.pageInicio:
                //Seleccionar el fragmento del inicio
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO)).commitNow();
                //break;
                return true;

            case R.id.pageHistorial:
                //Seleccionar el fragmento del historial
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO)).commitNow();
                //break;
                return true;

            case R.id.pageQuimicos:
                //Seleccionar el fragmento de los químicos
                fragmentManager.beginTransaction().hide(fragmentManager.findFragmentByTag(INICIO_FRAGMENTO))
                        .hide(fragmentManager.findFragmentByTag(HISTORIAL_FRAGMENTO))
                        .show(fragmentManager.findFragmentByTag(QUIMICOS_FRAGMENTO)).commitNow();
                //break;
                return true;
        }
        return false;
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.fragment_container_view);
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }*/
}
