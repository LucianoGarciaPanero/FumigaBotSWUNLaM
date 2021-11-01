package com.example.fumigabot.steps;

import android.content.ClipData;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.fumigabot.ItemViewModel;
import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Robot;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step2Fragment extends Fragment {

    private TextInputLayout listaQuimicos;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterListaQuimicos;
    private ArrayList<String> quimicosDisponibles;
    private ConstraintLayout panelMensaje;
    private String ultimoQuimico;

    private ItemViewModel viewModelQuimico;

    public Step2Fragment() {
        // Required empty public constructor
        super(R.layout.fragment_step2);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Transiciones en los cambios de fragmento
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

        quimicosDisponibles = (ArrayList<String>) getArguments().getSerializable("quimicos");
        ultimoQuimico = getArguments().getString("quimicoRobot");
        if(ultimoQuimico==null)
            ultimoQuimico="";

        //Decimos que este fragmento va a proveer info a la activity host (nueva fumigación)
        viewModelQuimico = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        viewModelQuimico.seleccionarQuimico(ultimoQuimico);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_step2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View vista = getView();
        //Instanciamos todos los elementos de la vista una vez que está creada
        panelMensaje = vista.findViewById(R.id.mensajeQuimico);
        listaQuimicos = vista.findViewById(R.id.listaQuimicos);
        autoCompleteTextView = vista.findViewById(R.id.autoCompleteTextView);
        ((AutoCompleteTextView)listaQuimicos.getEditText()).setOnItemClickListener(listaListener);

        //verificarFumigacion();
        viewModelQuimico.isInstantanea().observe(this, item -> {
            verificarFumigacion(item);
        });
    }

    private void verificarFumigacion(Boolean isInstantanea){
        if(isInstantanea == true){
            //Si es instantánea, no tenemos que dejarle cambiar el químico
            viewModelQuimico.seleccionarQuimico(ultimoQuimico);
            Log.i("test", "Verificar fumigacion: es instantanea");
            autoCompleteTextView.setEnabled(false);
            listaQuimicos.setEnabled(false);
            autoCompleteTextView.setText(ultimoQuimico, false);
            mostrarMensaje(View.VISIBLE);
        }
        else {
            Log.i("STEP", "Verificar fumigacion: NO es instantanea");
            autoCompleteTextView.setEnabled(true);
            listaQuimicos.setEnabled(true);
            mostrarMensaje(View.GONE);
        }
    }

    private void mostrarMensaje(int valor){
        panelMensaje.setVisibility(valor);
        if(valor==View.VISIBLE){
            //si lo muestra, habilito directamente el boton de siguiente
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        configurarAdapterListaQuimicos();
    }

    public AdapterView.OnItemClickListener listaListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //Tomamos el valor del químico seleccionado
                if(position != -1){
                    viewModelQuimico.seleccionarQuimico(adapterListaQuimicos.getItem(position));
                }
            }
        };

    public void configurarAdapterListaQuimicos() {
        adapterListaQuimicos = new ArrayAdapter<>(getContext(), R.layout.list_item, quimicosDisponibles);
        autoCompleteTextView.setAdapter(adapterListaQuimicos);
    }
}
