package com.example.fumigabot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.fumigabot.firebase.Fumigacion;

import java.util.List;

public class QuimicosAdapter extends BaseAdapter {

    private Context context;
    private List<String> quimicos;

    public QuimicosAdapter(Context context, List<String> quimicos)
    {
        this.context = context;
        this.quimicos = quimicos;
    }

    @Override
    public int getCount() {
        return quimicos.size();
    }

    @Override
    public Object getItem(int position) {
        return quimicos.get(position);
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
            convertView = inflater.inflate(R.layout.entrada_quimico, parent, false);
        }


        TextView nombreQuimico = convertView.findViewById(R.id.nombreQuimico);

        //Agregamos toda la data
        final String item = (String)getItem(position);
        nombreQuimico.setText(item);

        return convertView;
    }
}
