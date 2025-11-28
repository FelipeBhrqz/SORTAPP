package model;

public class Resultado {
    private String nombreProvincia;
    private int sufragantes;
    // Nuevos campos para soportar vuelta y año
    private int anio;
    private int vuelta;

    // Constructor original para compatibilidad
    public Resultado(String nombreProvincia, int sufragantes) {
        this.nombreProvincia = nombreProvincia;
        this.sufragantes = sufragantes;
    }

    // Constructor extendido con año y vuelta
    public Resultado(String nombreProvincia, int sufragantes, int anio, int vuelta) {
        this.nombreProvincia = nombreProvincia;
        this.sufragantes = sufragantes;
        this.anio = anio;
        this.vuelta = vuelta;
    }

    public String getNombreProvincia() {
        return nombreProvincia;
    }
    public int getSufragantes() {
        return sufragantes;
    }
    public void setNombreProvincia(String nombreProvincia) {
        this.nombreProvincia = nombreProvincia;
    }
    public void setSufragantes(int sufragantes) {
        this.sufragantes = sufragantes;
    }

    public int getAnio() {
        return anio;
    }
    public int getVuelta() {
        return vuelta;
    }
    public void setAnio(int anio) {
        this.anio = anio;
    }
    public void setVuelta(int vuelta) {
        this.vuelta = vuelta;
    }

    @Override
    public String toString() {
        // Incluye vuelta y año si están definidos (>0)
        String meta = (anio > 0 ? (" ["+anio+"]") : "") + (vuelta > 0 ? (" (V"+vuelta+")") : "");
        return nombreProvincia + ":" + sufragantes + meta;
    }
}