package com.intuitivecare.transformacao.model;

/**
 * Modelo para dados cadastrais de operadora
 */
public class OperadoraCadastro {

    private String registroANS;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String modalidade;
    private String uf;
    private String municipio;
    private String dataRegistro;

    // Getters e Setters
    public String getRegistroANS() {
        return registroANS;
    } // getRegistroANS

    public void setRegistroANS(String registroANS) {
        this.registroANS = registroANS;
    } // setRegistroANS

    public String getCnpj() {
        return cnpj;
    } // getCnpj

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    } // setCnpj

    /**
     * Retorna CNPJ normalizado (apenas números)
     */
    public String getCnpjNormalizado() {
        if (cnpj == null) return "";
        return cnpj.replaceAll("[^0-9]", "");
    } // getCnpjNormalizado

    public String getRazaoSocial() {
        return razaoSocial;
    } // getRazaoSocial

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    } // setRazaoSocial

    public String getNomeFantasia() {
        return nomeFantasia;
    } // getNomeFantasia

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
    } // setNomeFantasia

    public String getModalidade() {
        return modalidade;
    } // getModalidade

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    } // setModalidade

    public String getUf() {
        return uf;
    } // getUf

    public void setUf(String uf) {
        this.uf = uf;
    } // setUf

    public String getMunicipio() {
        return municipio;
    } // getMunicipio

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    } // setMunicipio

    public String getDataRegistro() {
        return dataRegistro;
    } // getDataRegistro

    public void setDataRegistro(String dataRegistro) {
        this.dataRegistro = dataRegistro;
    } // setDataRegistro
} // OperadoraCadastro
