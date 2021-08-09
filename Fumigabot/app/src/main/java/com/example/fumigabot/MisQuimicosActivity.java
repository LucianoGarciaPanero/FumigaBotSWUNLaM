package com.example.fumigabot;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fumigabot.firebase.MyFirebase;
import com.example.fumigabot.firebase.Robot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MisQuimicosActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference reference;
    private Robot robot;
    private TableLayout tablaQuimicos;
    private EditText txtNuevoQuimico;
    private Button btnAgregarQuimico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_quimicos);

        //Obtiene el robot
        robot = (Robot) getIntent().getSerializableExtra("robot");

        //Instancia y referencia de la BD en Firebase
        firebaseDatabase = MyFirebase.getInstance();
        reference = firebaseDatabase.getReference("robots/" + robot.getRobotId());
        //Para que se mantenga sincronizado offline
        reference.keepSynced(true);
        reference.addValueEventListener(robotEventListener);

        tablaQuimicos = findViewById(R.id.tablaQuimicos);
        btnAgregarQuimico = findViewById(R.id.btnAgregarQuimico);
        btnAgregarQuimico.setOnClickListener(btnAgregarQuimicoListener);
    }

    private View.OnClickListener btnAgregarQuimicoListener = v -> {
        String nuevoQuimico = txtNuevoQuimico.getText().toString();
        robot.getQuimicosDisponibles().add(nuevoQuimico);
        updateRobot(robot);
        txtNuevoQuimico.setText("");
        Toast.makeText(getApplicationContext(), "¡Químico agregado!", Toast.LENGTH_SHORT).show();
    };

    public void updateRobot(Robot robot) {
        reference.child("quimicosDisponibles").setValue(robot.getQuimicosDisponibles());
    }

    private ValueEventListener robotEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            robot = dataSnapshot.getValue(Robot.class);
            // Limpiamos todas las filas de fumigaciones anteriores
            // ya que si no, se van a agregar repetidas
            if (tablaQuimicos.getChildCount() > 1)
                tablaQuimicos.removeViews(1, tablaQuimicos.getChildCount() - 1);
            generarTablaQuimicos(robot.getQuimicosDisponibles());
        }
        @Override
        public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w("WTF", "Failed to read value.", error.toException());
        }
    };

    public void generarTablaQuimicos(ArrayList<String> listaQuimicos) {
        for(String quimico : listaQuimicos)
            agregarFila(quimico);
        agregarFilaEditable(); // Quizás lo podemos sacar y dejarlo estático en el xml
    }

    public void agregarFila(String quimico){
        // Crea nueva fila
        TableRow nuevaFila = new TableRow(this);
        nuevaFila.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT));
        nuevaFila.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        // View del químico
        TextView textQuimico = new TextView(this);
        textQuimico.setText(quimico);
        textQuimico.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textQuimico.setTextColor(getResources().getColor(R.color.colorPrimary));
        int value = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
        textQuimico.setPadding(value, value, value, value);

        nuevaFila.addView(textQuimico);
        tablaQuimicos.addView(nuevaFila, new TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT));
    }

    public void agregarFilaEditable(){
        // Crea nueva fila
        TableRow nuevaFila = new TableRow(this);
        nuevaFila.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT));
        nuevaFila.setBackgroundColor(getResources().getColor(R.color.colorAccentFilas));

        // EditText para nuevo quimico
        txtNuevoQuimico = new EditText(this);
        txtNuevoQuimico.setHint("ingresá un nuevo químico");
        txtNuevoQuimico.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        txtNuevoQuimico.setTextSize(14);
        txtNuevoQuimico.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        int value = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
        txtNuevoQuimico.setPadding(value, value, value, value);

        nuevaFila.addView(txtNuevoQuimico);
        tablaQuimicos.addView(nuevaFila, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onDestroy() {
        reference.removeEventListener(robotEventListener);
        super.onDestroy();
    }
}
