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
    private Robot robot;

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
        robot = (Robot) getArguments().getSerializable("quimicoRobot");
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
        listaQuimicos = vista.findViewById(R.id.listaQuimicos);
        autoCompleteTextView = vista.findViewById(R.id.autoCompleteTextView);
        ((AutoCompleteTextView)listaQuimicos.getEditText()).setOnItemClickListener(listaListener);

        //Decimos que este fragmento va a proveer info a la activity host (nueva fumigación)
        viewModelQuimico = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        //verificarFumigacion();
        viewModelQuimico.isInstantanea().observe(this, item -> {
            verificarFumigacion(item);
        });
    }

    private void verificarFumigacion(Boolean isInstantanea){
        if(isInstantanea == true){
            //Si es instantánea, no tenemos que dejarle cambiar el químico
            //viewModelQuimico.seleccionarQuimico(new ClipData.Item(robot.getUltimoQuimico()));
            Log.i("STEP", "Verificar fumigacion: es instantanea");
            autoCompleteTextView.setEnabled(false);
            listaQuimicos.setEnabled(false);
        }
        else {
            Log.i("STEP", "Verificar fumigacion: NO es instantanea");
            autoCompleteTextView.setEnabled(true);
            listaQuimicos.setEnabled(true);
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
                    viewModelQuimico.seleccionarQuimico(new ClipData.Item(adapterListaQuimicos.getItem(position)));
                }
            }
        };

    public void configurarAdapterListaQuimicos() {
        adapterListaQuimicos = new ArrayAdapter<>(getContext(), R.layout.list_item, quimicosDisponibles);
        autoCompleteTextView.setAdapter(adapterListaQuimicos);
    }
}
