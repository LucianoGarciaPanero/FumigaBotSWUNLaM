package com.example.fumigabot;

import android.content.ClipData;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemViewModel extends ViewModel {

    //Esta clase la necesitamos para hacer el envío de informacion entre los fragmentos y la activity host (Nueva fumigación)

    private final MutableLiveData<ClipData.Item> quimicoSeleccionado = new MutableLiveData<>();
    private final MutableLiveData<ClipData.Item> cantidadSeleccionada = new MutableLiveData<>();

    public void seleccionarQuimico(ClipData.Item item) {
        quimicoSeleccionado.setValue(item);
    }

    public LiveData<ClipData.Item> getQuimicoSeleccionado() {
        return quimicoSeleccionado;
    }

    public void seleccionarCantidad(ClipData.Item item) {
        cantidadSeleccionada.setValue(item);
    }

    public LiveData<ClipData.Item> getCantidadSeleccionada() {
        return cantidadSeleccionada;
    }
}