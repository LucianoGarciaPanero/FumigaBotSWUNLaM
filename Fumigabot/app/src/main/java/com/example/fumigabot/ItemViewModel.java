package com.example.fumigabot;

import android.content.ClipData;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Date;

public class ItemViewModel extends ViewModel {

    //Esta clase la necesitamos para hacer el envío de informacion entre los fragmentos y la activity host (Nueva fumigación)

    private final MutableLiveData<String> quimicoSeleccionado = new MutableLiveData<>();
    private final MutableLiveData<ClipData.Item> cantidadSeleccionada = new MutableLiveData<>();
    private final MutableLiveData<Date> horarioSeleccionado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> iniciarAhora = new MutableLiveData<>();
    private final MutableLiveData<Boolean> repetirDiariamente = new MutableLiveData<>();
    private final MutableLiveData<Boolean> disponible = new MutableLiveData<>();

    public void seleccionarQuimico(String valor) { quimicoSeleccionado.setValue(valor); }

    public LiveData<String> getQuimicoSeleccionado() {
        return quimicoSeleccionado;
    }

    public void seleccionarCantidad(ClipData.Item item) {
        cantidadSeleccionada.setValue(item);
    }

    public LiveData<ClipData.Item> getCantidadSeleccionada() {
        return cantidadSeleccionada;
    }

    public void seleccionarHorario(Date fecha) {
        horarioSeleccionado.setValue(fecha);
    }

    public LiveData<Date> getHorarioSeleccionado() {
        return horarioSeleccionado;
    }

    public void setInstantanea(Boolean valor) {
        iniciarAhora.setValue(valor);
    }

    public LiveData<Boolean> isInstantanea() {
        return iniciarAhora;
    }

    public void setRepetirDiariamente(Boolean repetir){
        repetirDiariamente.setValue(repetir);
    }

    public LiveData<Boolean> getRepetirDiariamente() {
        return repetirDiariamente;
    }

    public void setDisponible(){
        if(quimicoSeleccionado.getValue() != null
                && cantidadSeleccionada.getValue() != null
                && horarioSeleccionado.getValue() != null
                && iniciarAhora.getValue() != null
                && repetirDiariamente.getValue() != null)
            disponible.setValue(true);

        else
            disponible.setValue(false);
    }

    public LiveData<Boolean> isDisponible() {
        return disponible;
    }
}