package com.example.fumigabot.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.fumigabot.ItemViewModel;
import com.example.fumigabot.R;
import com.google.android.material.transition.MaterialSharedAxis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step4Fragment extends Fragment {

    private ItemViewModel viewModelResumen;
    private TextView quimicoSeleccionado;
    private TextView cantidadSeleccionada;
    private TextView horarioSeleccionado;

    public Step4Fragment() {
        // Required empty public constructor
        super(R.layout.fragment_step4);
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
        return inflater.inflate(R.layout.fragment_step4, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        quimicoSeleccionado = vista.findViewById(R.id.quimicoSeleccionado);
        cantidadSeleccionada = vista.findViewById(R.id.cantidadSeleccionada);
        horarioSeleccionado = vista.findViewById(R.id.horarioSeleccionado);
        viewModelResumen = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        viewModelResumen.isDisponible().observe(requireActivity(), item -> {
            if(item != null && item.booleanValue())
                cargarDatos();
        });
    }

    private void cargarDatos(){
        String horarios;
        Long fechaHora = viewModelResumen.getHorarioSeleccionado().getValue().getTime();

        quimicoSeleccionado.setText(viewModelResumen.getQuimicoSeleccionado().getValue().getText());
        cantidadSeleccionada.setText(viewModelResumen.getCantidadSeleccionada().getValue().getText());
        if(viewModelResumen.isInstantanea().getValue())
            horarioSeleccionado.setText("Inicia ahora");
        else {
            horarios = new SimpleDateFormat("dd MMMM YYYY", Locale.getDefault())
                    .format(fechaHora);

            horarios += " a las " + new SimpleDateFormat("HH:mm")
                    .format(fechaHora);

            if(viewModelResumen.getRepetirDiariamente().getValue().booleanValue())
                horarios += "\n\nRepetir cada "
                        + getDiaSemana(fechaHora);

            horarioSeleccionado.setText(horarios);
        }
    }

    private String getDiaSemana(Long fecha){
        String resultado = new SimpleDateFormat("EEEE", Locale.getDefault())
                .format(fecha);
        return resultado;
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
