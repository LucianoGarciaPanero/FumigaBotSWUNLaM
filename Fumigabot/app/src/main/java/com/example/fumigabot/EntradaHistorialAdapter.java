package com.example.fumigabot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;

import org.w3c.dom.Text;

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


        //Agregamos toda la data
        final Fumigacion item = (Fumigacion)getItem(position);

        //Le damos el formato que queremos
        String fechaInicio = item.darFormatoFechaInicio();
        String horaInicio = item.getHoraInicio();
        String horaFin = item.getHoraFin();

        fechaFumigacion.setText(fechaInicio);
        duracion.setText(item.calcularDuracion().trim());

        //Fila2:
        String quimico;
        if(item.getQuimicoUtilizado() == null)
            quimico = "No especificado";
        else
            quimico = item.getQuimicoUtilizado().trim();

        String hora_quimico = horaInicio + " a " + horaFin + ", " + quimico;
        horaYQuimico.setText(hora_quimico.trim());

        return convertView;
    }
}
