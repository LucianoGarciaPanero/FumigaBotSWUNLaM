package com.example.fumigabot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.firebase.MyFirebase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntradaProgramadaAdapter extends BaseAdapter {

    private Context context;
    private int robotId;
    private List<Fumigacion> fumigacionesProgramadas;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private FirebaseFunctions functions;

    public EntradaProgramadaAdapter(Context context, int idRobot, List<Fumigacion> fumigacionesProgramadas) {
        this.context = context;
        this.robotId = idRobot;
        this.fumigacionesProgramadas = fumigacionesProgramadas;
        this.functions = MyFirebase.getFunctionsInstance();
        this.firebaseDatabase = MyFirebase.getDatabaseInstance();
        this.reference = this.firebaseDatabase.getReference("fumigaciones_programadas/" + robotId);
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

            TextView fechaFumigacion = convertView.findViewById(R.id.fechaFumigacion);
            TextView horaQuimico = convertView.findViewById(R.id.horaQuimico);
            ConstraintLayout layoutDiasRecurrencia = convertView.findViewById(R.id.layoutDiasRecurrencia);
            TextView txtDomingo = convertView.findViewById(R.id.txtDomingo);
            TextView txtLunes = convertView.findViewById(R.id.txtLunes);
            TextView txtMartes = convertView.findViewById(R.id.txtMartes);
            TextView txtMiercoles = convertView.findViewById(R.id.txtMiercoles);
            TextView txtJueves= convertView.findViewById(R.id.txtJueves);
            TextView txtViernes = convertView.findViewById(R.id.txtViernes);
            TextView txtSabado = convertView.findViewById(R.id.txtSabado);
            Switch switchActivada = convertView.findViewById(R.id.switchAlarma);
            ImageButton borrar = convertView.findViewById(R.id.imgBorrarProgramada);

            //Agregamos toda la data
            final Fumigacion item = (Fumigacion) getItem(position);

            //Log.i("test", "fumigacion: " + item);


            //Le damos el formato que queremos
            String fechaInicio = "";
            if(item.isRecurrente()){
                fechaInicio = item.darFormatoFechaInicio();
                Date fecha = new Date(Long.parseLong(item.getTimestampInicio()));
                //Log.i("test", "Numero del dia de timestamp inicio: " + fecha.getDay());
                int color = convertView.getResources().getColor(R.color.green_200);
                switch(fecha.getDay()){
                    case 0:
                        txtDomingo.setTextColor(color);
                        break;
                    case 1:
                        txtLunes.setTextColor(color);
                        break;
                    case 2:
                        txtMartes.setTextColor(color);
                        break;
                    case 3:
                        txtMiercoles.setTextColor(color);
                        break;
                    case 4:
                        txtJueves.setTextColor(color);
                        break;
                    case 5:
                        txtViernes.setTextColor(color);
                        break;
                    case 6:
                        txtSabado.setTextColor(color);
                        break;
                    default:
                        break;
                }
            } else {
                fechaInicio = item.formatoFechaInicioOneTime();
                switchActivada.setVisibility(View.GONE);
                //ocultar dias de recurrencia
                layoutDiasRecurrencia.setVisibility(View.GONE);
            }

            String horaInicio = item.getHoraInicio();

            fechaFumigacion.setText(fechaInicio);

            String quimico;
            if (item.getQuimicoUtilizado() == null)
                quimico = "No especificado";
            else
                quimico = item.getQuimicoUtilizado().trim();

            String hora_quimico = horaInicio + ", " + quimico;
            horaQuimico.setText(hora_quimico.trim());

            switchActivada.setChecked(item.isActiva());
            switchActivada.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        //Log.i("Adapter", "activar");
                        //is activa = true
                        activar(item, true);

                    }
                    else {
                        //Log.i("Adapter", "desactivar");
                        //is activa = false
                        activar(item, false);
                    }
                }
            });

            borrar.setOnClickListener(v -> {
                borrarProgramada(item);
            });

            return convertView;
        }
        catch(Exception e){
            //temp
            return convertView;
        }
    }

    private void activar(Fumigacion fumigacion, boolean valor){
        fumigacion.setActiva(valor);
        reference.child(fumigacion.getFumigacionId()).setValue(fumigacion.toMapProgramada());
    }

    private void borrarProgramada(Fumigacion fumigacion){
        //reference.child(fumigacion.getFumigacionId()).removeValue(); //setValue(fumigacion.toMapProgramada());
        //update con funcion de detener fumigacion
        HashMap<String, Object> params = new HashMap<>();
        params.put("robotId",robotId);
        params.put("fumigacionId", fumigacion.getFumigacionId());
        funcBorrarProgramada(params).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                String resultado = "";

                if(task.isComplete()) {

                    if (task.isSuccessful()) {
                        resultado = task.getResult();
                        if (resultado.equalsIgnoreCase("Ok")) {
                            resultado = "Se eliminó la fumigación";
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Object details = ffe.getDetails();
                            Log.i("test", "Firebase Functions Exception: Code " + code);

                        }
                        resultado = e.getMessage();
                    }
                    //cambiar la forma en que informa al user que no lo hizo
                    //runOnUiThread(Toast.makeText(getApplicationContext(), resultado, Toast.LENGTH_SHORT)::show);
                    Log.i("test", "resultado: " + resultado);
                    Toast.makeText(context, resultado, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Task<String> funcBorrarProgramada(Map<String, Object> params){
        //invocamos a la Function
        return functions.getHttpsCallable("programadaDelete")
                .call(params)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        //esta continuacion se ejecuta en caso de éxito o falla, pero si la Task
                        //falla, entonces gtResult() va a arrojar una excepción la cual se va a propagar
                        String resultado = (String) task.getResult().getData();
                        Log.i("test", "Task detener fumigacion retorna resultado: " + resultado);
                        return resultado;
                    }
                });
    }
}