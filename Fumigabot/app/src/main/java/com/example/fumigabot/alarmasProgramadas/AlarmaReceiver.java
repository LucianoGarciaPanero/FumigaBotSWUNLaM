package com.example.fumigabot.alarmasProgramadas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.fumigabot.firebase.Fumigacion;
import com.example.fumigabot.home.InicioFragment;
import java.util.Set;

/**Broadcast Receiver de las alarmas creadas.
 * El método onReceive() se ejecuta cuando llega la fecha y hora de la  fumigación programda.
 * */
public class AlarmaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //Acá tengo que hacer lo que quiero hacer cuando se ejecuta la alarma
        //O sea, lo que queremos que haga es que inicie una fumigación: que pase de programada a ejecutarse

        //Entonces: primero, la tengo que ir a buscar
        //Probe pasando el string de time stamp inicio a ver si lo recibe
        Bundle bundleFumigacion = intent.getBundleExtra("fumigacion_bundle");
        if(bundleFumigacion == null){
            Log.i("ALARMA", "Bundle fumigacion es NULL");
            return;
        }

        //Tengo la fumigación programada
        Fumigacion fumigacion = (Fumigacion) bundleFumigacion.getSerializable("fumigacion");

        //Busco las que guardé, que significa que tengo que ejecutarlas
        //NOTA: acá falta mantener actualizado esto en cuanto modificaciones o eliminaciones
        //de programadas.
        Set<String> creadas = Alarma.getAlarmasCreadas(context);
        if(creadas == null) {
            Log.i("ALARMA", "get alarmas creadas es NULL");
            return;
        }
        for(String creada : creadas){
            if(creada.contains(fumigacion.getTimestampInicio())){
                //Si cumple, la tengo que ejecutar
                InicioFragment.Instance.iniciarFumigacion(fumigacion);
                Log.i("ALARMA", "Alarma BR empezó a fumigar");
                return;
            }
        }
        Log.i("ALARMA", "Alarma Receiver no encontró nada para iniciar");
        Toast.makeText(context, "AlarmRec no encontró nada", Toast.LENGTH_LONG).show();
    }
}