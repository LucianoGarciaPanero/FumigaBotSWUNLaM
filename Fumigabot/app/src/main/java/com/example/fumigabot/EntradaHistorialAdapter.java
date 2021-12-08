package com.example.fumigabot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.fumigabot.firebase.Fumigacion;

import java.util.List;

public class EntradaHistorialAdapter extends BaseAdapter {

    private Context context;
    private List<Fumigacion> fumigaciones;

    public EntradaHistorialAdapter(Context context, List<Fumigacion> fumigaciones)
    {
        this.context = context;
        this.fumigaciones= fumigaciones;
    }

    @Override
    public int getCount() {
        return fumigaciones.size();
    }

    @Override
    public Object getItem(int position) {
        return fumigaciones.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.entrada_historial, parent, false);
        }

        TextView fechaFumigacion = convertView.findViewById(R.id.fechaFumigacion);
        TextView duracion = convertView.findViewById(R.id.duracion);
        TextView horaYQuimico = convertView.findViewById(R.id.horaQuimico);

        ImageView alerta = convertView.findViewById(R.id.alerta);
        ImageView alertaMaxima = convertView.findViewById(R.id.alertaMaxima);

        //Agregamos toda la data
        final Fumigacion item = (Fumigacion)getItem(position);

        //Le damos el formato que queremos
        String fechaInicio = item.darFormatoFechaInicio();
        String horaInicio = item.getHoraInicio();
        String horaFin = item.getHoraFin();

        String quimico;
        if(item.getQuimicoUtilizado() == null)
            quimico = "No especificado";
        else
            quimico = item.getQuimicoUtilizado().trim();

        String hora_quimico = horaInicio + " a " + horaFin + ", " + quimico;

        if(item.getObservaciones().toLowerCase().equals("ok")){
            //si es ok, está todo bien
            duracion.setText(item.calcularDuracion().trim());
            alerta.setVisibility(View.GONE);
        }
        else if(item.getTimestampFin().equals("0")){
            //mostrar solamente !!!!!!
            duracion.setVisibility(View.GONE);
            alerta.setVisibility(View.GONE);
            alertaMaxima.setVisibility(View.VISIBLE);
            hora_quimico = horaInicio + ", " + quimico;
        } else {
            //mostrar duracion + !!
            alerta.setVisibility(View.VISIBLE);
            duracion.setVisibility(View.VISIBLE);
            alertaMaxima.setVisibility(View.GONE);
            duracion.setText(item.calcularDuracion().trim());
        }

        horaYQuimico.setText(hora_quimico.trim());
        fechaFumigacion.setText(fechaInicio);

        return convertView;
    }
}