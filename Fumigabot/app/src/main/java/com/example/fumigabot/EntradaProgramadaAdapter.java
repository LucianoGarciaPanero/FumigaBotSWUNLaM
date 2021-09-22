package com.example.fumigabot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;

import java.util.List;

public class EntradaProgramadaAdapter extends BaseAdapter {

    private Context context;
    private List<Fumigacion> fumigacionesProgramadas;

    public EntradaProgramadaAdapter(Context context, List<Fumigacion> fumigacionesProgramadas) {
        this.context = context;
        this.fumigacionesProgramadas = fumigacionesProgramadas;
    }

    @Override
    public int getCount() {
        return fumigacionesProgramadas.size();
    }

    @Override
    public Object getItem(int position) {
        return fumigacionesProgramadas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.entrada_programadas, parent, false);
            }

            TextView fechaFumigacion = convertView.findViewById(R.id.fechaFumigacion);;
            TextView horaQuimico = convertView.findViewById(R.id.horaQuimico);

            //Agregamos toda la data
            final Fumigacion item = (Fumigacion) getItem(position);

            //Le damos el formato que queremos
            String fechaInicio = item.darFormatoFechaInicio();
            String horaInicio = item.getHoraInicio();

            fechaFumigacion.setText(fechaInicio);

            //Fila2:
            String quimico;
            if (item.getQuimicoUtilizado() == null)
                quimico = "No especificado";
            else
                quimico = item.getQuimicoUtilizado().trim();

            String hora_quimico = "A las " + horaInicio + ", " + quimico;
            horaQuimico.setText(hora_quimico.trim());

            return convertView;
        }
        catch(Exception e){
            //temp
            return convertView;
        }
    }
}