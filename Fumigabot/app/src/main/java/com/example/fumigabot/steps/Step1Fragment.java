package com.example.fumigabot.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.fumigabot.ItemViewModel;
import com.example.fumigabot.R;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step1Fragment extends Fragment {

    private Switch switchComenzar;
    private CalendarView calendario;
    private TimePicker timePicker;
    private CheckBox repetirDiariamente;
    private ConstraintLayout layoutProgramar;
    private Date fecha;
    private ItemViewModel viewModelHorario;
    private boolean fumigando;
    private ConstraintLayout panelMensaje;

    public Step1Fragment() {
        // Required empty public constructor
        super(R.layout.fragment_step1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Transiciones en los cambios de fragmento
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

        fumigando =  getArguments().getBoolean("fumigando");
    }

    private void mostrarAlertaFumigando(boolean comenzarAhora){
        if(comenzarAhora) {
            if (fumigando == true) {
                panelMensaje.setVisibility(View.VISIBLE);
            }
        } else {
            panelMensaje.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_step1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        switchComenzar = vista.findViewById(R.id.switchComenzar);
        calendario = vista.findViewById(R.id.calendario);
        timePicker = vista.findViewById(R.id.horaPicker);
        repetirDiariamente = vista.findViewById(R.id.checkEsRecurrente);
        layoutProgramar = vista.findViewById(R.id.layoutProgramar);

        switchComenzar.setOnCheckedChangeListener(switchComenzarListener);
        calendario.setOnDateChangeListener(calendarioListener);
        timePicker.setOnTimeChangedListener(timeListener);
        repetirDiariamente.setOnCheckedChangeListener(repetirDiariamenteListener);

        fecha = new Date(calendario.getDate());
        viewModelHorario = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        //Por defecto dejamos seleccionado "comenzar ahora", por lo tanto, en principio, es instantánea
        viewModelHorario.setInstantanea(true);
        viewModelHorario.seleccionarHorario(fecha);
        viewModelHorario.setRepetirDiariamente(false);

        panelMensaje = vista.findViewById(R.id.panelMensajeFumigando);
        mostrarAlertaFumigando(true);
    }

    private CalendarView.OnDateChangeListener calendarioListener = new CalendarView.OnDateChangeListener() {
        @Override
        public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
            //Log.d("STEP3", dayOfMonth + " " + month + " " + year);
            fecha = new Date(year  - 1900, month, dayOfMonth);
            viewModelHorario.seleccionarHorario(fecha);
        }
    };

    private CompoundButton.OnCheckedChangeListener switchComenzarListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            viewModelHorario.setInstantanea(isChecked);
            habilitarProgramacion(!isChecked);
            mostrarAlertaFumigando(isChecked);
        }
    };

    private TimePicker.OnTimeChangedListener timeListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            //Log.d("STEP3", hourOfDay + ":" + minute);
            fecha.setHours(hourOfDay);
            fecha.setMinutes(minute);
            fecha.setSeconds(0);

            viewModelHorario.seleccionarHorario(fecha);
        }
    };

    private CompoundButton.OnCheckedChangeListener repetirDiariamenteListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            viewModelHorario.setRepetirDiariamente(isChecked);
        }
    };


    private void habilitarProgramacion(boolean valor){
        if(!valor)
            layoutProgramar.setVisibility(View.GONE);
        else
            layoutProgramar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
