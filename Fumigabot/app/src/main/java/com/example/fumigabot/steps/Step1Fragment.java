package com.example.fumigabot.steps;

import android.content.ClipData;
import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step1Fragment extends Fragment {

    private TextInputLayout listaQuimicos;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterListaQuimicos;
    private ArrayList<String> quimicosDisponibles;

    private ItemViewModel viewModelQuimico;

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

        quimicosDisponibles = (ArrayList<String>) getArguments().getSerializable("quimicos");
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
        listaQuimicos = vista.findViewById(R.id.listaQuimicos);
        autoCompleteTextView = vista.findViewById(R.id.autoCompleteTextView);
        ((AutoCompleteTextView)listaQuimicos.getEditText()).setOnItemClickListener(listaListener);

        //Decimos que este fragmento va a proveer los datos necesarios a la clase host (Nueva Fumigación)
        viewModelQuimico = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        configurarAdapterListaQuimicos();
    }

    public AdapterView.OnItemClickListener listaListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //Obtenemos el químico seleccionado
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
