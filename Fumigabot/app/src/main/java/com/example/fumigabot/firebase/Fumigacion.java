package com.example.fumigabot.firebase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Fumigacion implements Comparable {

    private String fumigacionId;
    private String timestampInicio;
    private String timestampFin;
    private String quimicoUtilizado;
    private String cantidadQuimicoPorArea;

    public Fumigacion(){
        // Default constructor required for calls to DataSnapshot.getValue(Fumigacion.class)
    }

    public String getFumigacionId() { return fumigacionId; }

    public void setFumigacionId (String fumigacionId) { this.fumigacionId = fumigacionId; }

    public String getTimestampInicio() { return timestampInicio; }

    public void setTimestampInicio (String timestampInicio) { this.timestampInicio = timestampInicio; }

    public String getTimestampFin() { return timestampFin; }

    public void setTimestampFin (String timestampFin) { this.timestampFin = timestampFin; }

    public String getQuimicoUtilizado() { return quimicoUtilizado; }

    public void setQuimicoUtilizado(String quimicoUtilizado) { this.quimicoUtilizado = quimicoUtilizado; }

    public String getCantidadQuimicoPorArea() { return cantidadQuimicoPorArea; }

    public void setCantidadQuimicoPorArea(String cantidadQuimicoPorArea) {
        this.cantidadQuimicoPorArea = cantidadQuimicoPorArea;
    }

    // Ordena descendentemente según timestampInicio
    @Override
    public int compareTo(Object o) {
        String timestampInicioCmp = ((Fumigacion) o).getTimestampInicio();
        return timestampInicioCmp.compareTo(this.timestampInicio);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("timestampFin", timestampFin);
        hashMap.put("timestampInicio", timestampInicio);
        hashMap.put("quimicoUtilizado", quimicoUtilizado);
        hashMap.put("cantidadQuimicoPorArea", cantidadQuimicoPorArea);
        return hashMap;
    }
}
