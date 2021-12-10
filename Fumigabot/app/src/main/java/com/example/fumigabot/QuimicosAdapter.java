package com.example.fumigabot;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuimicosAdapter extends BaseAdapter {

    private Context context;
    private Robot robot;
    private List<String> quimicos;
    private FirebaseFunctions functions;
    private MaterialAlertDialogBuilder builder;
    private AlertDialog alertDialog;

    public QuimicosAdapter(Context context, Robot robot, List<String> quimicos) {
        this.context = context;
        this.robot = robot;
        this.quimicos = quimicos;
        this.functions = MyFirebase.getFunctionsInstance();
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
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.entrada_quimico, parent, false);
        }

        TextView nombreQuimico = convertView.findViewById(R.id.nombreQuimico);
        ImageButton btnBorrarQuimico = convertView.findViewById(R.id.btnBorrarQuimico);
        ImageView imgQuimicoSeleccionado = convertView.findViewById(R.id.imgQuimicoSeleccionado);

        //Agregamos toda la data
        final String item = (String)getItem(position);
        nombreQuimico.setText(item);

        if(robot.getUltimoQuimico().equals(item)){
            imgQuimicoSeleccionado.setVisibility(View.VISIBLE);
            btnBorrarQuimico.setVisibility(View.GONE);
        } else{
            imgQuimicoSeleccionado.setVisibility(View.GONE);
            btnBorrarQuimico.setVisibility(View.VISIBLE);
        }

        btnBorrarQuimico.setOnClickListener(v -> {
            borrarQuimicoAlertDialog(robot, item);
        });

        return convertView;
    }

    private void borrarQuimicoAlertDialog(Robot robot, String quimico) {
        builder = new MaterialAlertDialogBuilder(context);

        builder.setMessage("Borrar un químico implica eliminar las fumigaciones programadas pendientes de ejecución que lo usen.\n\n¿Desea continuar?");

        builder.setPositiveButton("continuar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                borrarQuimico(robot, quimico);
            }
        });

        builder.setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void borrarQuimico(Robot robot, String quimico){
        HashMap<String, Object> params = new HashMap<>();
        params.put("robotId", robot.getRobotId());
        params.put("quimico", quimico);
        funcBorrarQuimico(params).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                String resultado = "";

                if(task.isComplete()) {
                    if (task.isSuccessful()) {
                        resultado = task.getResult();
                        if (resultado.equalsIgnoreCase("Ok")) {
                            resultado = "Se eliminó el químico";
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Log.i("test", "Firebase Functions Exception: Code " + code);

                        }
                        resultado = e.getMessage();
                    }
                    Log.i("test", "borrar quimico resultado: " + resultado);
                    Toast.makeText(context, resultado, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Task<String> funcBorrarQuimico(Map<String, Object> params){
        return functions.getHttpsCallable("borrarQuimico")
                .call(params)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        String resultado = (String) task.getResult().getData();
                        Log.i("test", "Task borrar quimico retorna resultado: " + resultado);
                        return resultado;
                    }
                });
    }
}