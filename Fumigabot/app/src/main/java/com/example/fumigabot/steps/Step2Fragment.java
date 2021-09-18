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
public class Step2Fragment extends Fragment {

    private TextInputLayout listaCantidadPorArea;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> adapterListaCantidadPorArea;

    private ItemViewModel viewModelCantidad;

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
        listaCantidadPorArea = vista.findViewById(R.id.listaCantidadArea);
        autoCompleteTextView = vista.findViewById(R.id.autoCompleteTextView2);
        ((AutoCompleteTextView)listaCantidadPorArea.getEditText()).setOnItemClickListener(listaListener);

        viewModelCantidad = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        configurarAdapterListaCantidadPorArea();
    }

    public AdapterView.OnItemClickListener listaListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if(position != -1){
                viewModelCantidad.seleccionarCantidad(new ClipData.Item(adapterListaCantidadPorArea.getItem(position)));
            }
        }
    };

    public void configurarAdapterListaCantidadPorArea(){
        adapterListaCantidadPorArea = new ArrayAdapter<>(getContext(), R.layout.list_item,
                getResources().getStringArray(R.array.cantidad_quimico_por_area));
        autoCompleteTextView.setAdapter(adapterListaCantidadPorArea);
    }
}
