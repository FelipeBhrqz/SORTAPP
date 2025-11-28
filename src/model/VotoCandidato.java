package model;

public class VotoCandidato implements Comparable<VotoCandidato> {
    private String nombreCandidato;
    private int votos;

    public VotoCandidato(String nombreCandidato, int votos) {
        this.nombreCandidato = nombreCandidato;
        this.votos = votos;
    }

    public String getNombreCandidato() {
        return nombreCandidato;
    }

    public int getVotos() {
        return votos;
    }

    public void setNombreCandidato(String nombreCandidato) {
        this.nombreCandidato = nombreCandidato;
    }

    public void setVotos(int votos) {
        this.votos = votos;
    }

    @Override
    public String toString() {
        return nombreCandidato + ":" + votos;
    }

    @Override
    public int compareTo(VotoCandidato o) {
        // ordenar ascendente por votos
        return Integer.compare(this.votos, o.votos);
    }
}