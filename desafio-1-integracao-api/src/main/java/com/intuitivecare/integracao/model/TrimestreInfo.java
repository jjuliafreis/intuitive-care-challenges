package com.intuitivecare.integracao.model;

/**
 * Representa informações de um trimestre disponível na API ANS
 */
public class TrimestreInfo {

    private final int ano;
    private final int trimestre;
    private final String url;

    public TrimestreInfo(int ano, int trimestre, String url) {
        this.ano = ano;
        this.trimestre = trimestre;
        this.url = url;
    } // TrimestreInfo

    public int getAno() {
        return ano;
    } // getAno

    public int getTrimestre() {
        return trimestre;
    } // getTrimestre

    public String getUrl() {
        return url;
    } // getUrl

    public String getIdentificador() {
        // Formato: "2024/3T" para ser usado na construção da URL
        return String.format("%d/%dT", ano, trimestre);
    } // getIdentificador

    @Override
    public String toString() {
        return String.format("Trimestre %d/%d", trimestre, ano);
    } // toString

    /**
     * Comparador para ordenação por data (mais recente primeiro)
     */
    public int compareToDesc(TrimestreInfo other) {
        int anoCompare = Integer.compare(other.ano, this.ano);
        if (anoCompare != 0) return anoCompare;
        return Integer.compare(other.trimestre, this.trimestre);
    } // compareToDesc
} // TrimestreInfo
