package com.example.fumigabot.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.fumigabot.R;
import com.example.fumigabot.firebase.Fumigacion;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.transition.MaterialSharedAxis;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class Step3Fragment extends Fragment {

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
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}
