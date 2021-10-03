package com.example.fumigabot.alarmasProgramadas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.example.fumigabot.firebase.Fumigacion;
import java.util.HashSet;
import java.util.Set;


/**Esta clase sirve para crear y configurar todas las alarmas necesarias para
 * las fumigaciones programadas.
 *
 * @version 1.0*/


public class Alarma {
    //Armamos este ID falopa a ver si sirve de algo
    private static int alarmaId = 0;
    private static Set<String> alarmasCreadas = new HashSet<>();

    private int id;
    private Fumigacion fumigacion;

    private Alarma(int id, Fumigacion fumigacion){
        this.id = id;
        this.fumigacion = fumigacion;
    }


    /**Crea una alarma para una fumigación programada en su fecha y hora correspondiente.
     * La alarma se dispara sola a la fecha y hora establecida por la fumigación.
     * Lo que ejecutará la alarma se encuentra determinado por la clase AlarmaReceiver.
     *
     * Esta función invoca a guardarAlarmaCreada() para llevar un control (ver función).
     *
     * @param context: contexto de donde se invoca.
     * @param fumigacion: fumigación para la cual se está creando la alarma.*/
    public static void empezarAlarma(Context context, Fumigacion fumigacion){
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmaReceiver.class);

        //Voy a probar con un bundle:
        Bundle bundle = new Bundle();
        bundle.putSerializable("fumigacion", fumigacion);
        intent.putExtra("fumigacion_bundle", bundle);

        //Pending intent params: (contexto, un número único x cada PI, intent de arriba, 0)
        //one shot es para que se ejecute una sola vez, cambiar cuando se quiera "repetir"
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ++alarmaId, intent, PendingIntent.FLAG_ONE_SHOT);

        //inicializar alarma: este metodo lanza en el tiempo exacto
        //se ejecuta en el tiempo dicho por c (en milis) y ejecuta lo del pending intent
        Long timeStampInicio = Long.parseLong(fumigacion.getTimestampInicio());
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeStampInicio, pendingIntent);

        Log.i("ALARMA", "Se empezó la alarma");
        guardarAlarmaCreada(context, timeStampInicio);
    }

    /***/
    public static void cancelarAlarma(Context context, int idAlarma){
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmaReceiver.class);
        //Pending intent params: (contexto, un número único x cada PI, intent de arriba, 0)
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, idAlarma, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
    }

    /**Guarda las alarmas que fueron creadas en SharedPreferences.
     * Esto se hace para eventualmente llevar un control de las alarmas que ya fueron creadas
     * para las fumigaciones programadas y saber, si eventualmente se eliminan o modifican fumigaciones
     * programadas, proceder a la modificación o eliminación de las alarmas ya creadas.
     *
     * @param context: contexto.
     * @param timeStampInicio: time stamp de inicio de la fumigación programada.*/
    private static void guardarAlarmaCreada(Context context, Long timeStampInicio){
        //Por el momento no me queda otra que usar SP
        //Creamos esta especie de ID falopa a ver si sirve de algo combinado
        String idAlarma = alarmaId + "-" + timeStampInicio;
        alarmasCreadas.add(idAlarma);

        SharedPreferences sp = context.getSharedPreferences("listado_alarmas_creadas", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        try{
            editor.putStringSet("alarmasCreadas", alarmasCreadas);
            Log.i("ALARMA", "Guardar alarma en SP: OK");
        } catch (Exception e){
            Log.i("ALARMA", "Guardar alarma en SP: " + e.getMessage());
        }
        editor.apply();
    }

    /**Obtiene todas las alarmas que fueron creadas y almacenadas en SharedPreferences.
     * Esta función es útil para el momento en el que tiene que ejecutarse una alarma y saber si
     * la fumigación programada fue modificada o eliminada.
     *
     * @param context: contexto.*/
    public static Set<String> getAlarmasCreadas(Context context){
        // load tasks from preference
        SharedPreferences sp = context.getSharedPreferences("listado_alarmas_creadas", Context.MODE_PRIVATE);
        Set<String> res = sp.getStringSet("alarmasCreadas", null); // new HashSet<>());
        return res;
    }
}
