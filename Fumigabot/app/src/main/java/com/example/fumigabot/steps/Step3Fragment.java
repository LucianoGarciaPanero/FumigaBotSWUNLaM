package com.example.fumigabot.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
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
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step3Fragment extends Fragment {

    private TimePicker timePicker;
    private CalendarView calendario;
    private Date fecha;
    private ItemViewModel viewModelHorario;
    private Switch switchComenzar;
    private ConstraintLayout layoutProgramar;

    public Step3Fragment() {
        // Required empty public constructor
        super(R.layout.fragment_step3);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Transiciones en los cambios de fragmento
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_step3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        calendario = vista.findViewById(R.id.calendario);
        fecha = new Date(calendario.getDate());
        timePicker = vista.findViewById(R.id.horaPicker);
        switchComenzar = vista.findViewById(R.id.switchComenzar);
        layoutProgramar = vista.findViewById(R.id.layoutProgramar);
        calendario.setOnDateChangeListener(calendarioListener);
        timePicker.setOnTimeChangedListener(timeListener);
        switchComenzar.setOnCheckedChangeListener(switchComenzarListener);

        viewModelHorario = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        viewModelHorario.setInstantanea(false);
    }

    private CalendarView.OnDateChangeListener calendarioListener = new CalendarView.OnDateChangeListener() {
        @Override
        public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
            //Log.d("STEP3", dayOfMonth + " " + month + " " + year);
            fecha = new Date(year  - 1900, month, dayOfMonth);
            viewModelHorario.seleccionarHorario(fecha);
        }
    };

    private TimePicker.OnTimeChangedListener timeListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            //Log.d("STEP3", hourOfDay + ":" + minute);
            fecha.setHours(hourOfDay);//(timePicker.getHour());
            fecha.setMinutes(minute);//(timePicker.getMinute());

            viewModelHorario.seleccionarHorario(fecha);
        }
    };

    private CompoundButton.OnCheckedChangeListener switchComenzarListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            viewModelHorario.setInstantanea(isChecked);
            habilitarProgramacion(!isChecked);
            //viewModelHorario.seleccionarHorario(new Date());
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
