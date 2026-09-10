package br.com.pfc.biblioteca.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Materia {
    TI ("Ti"),
    DIREITO ("Direito"),
    MEDICINA ("Medicina"),
    ODONTOLOGIA ("Odontologia"),
    VETERINARIA ("Veterinária"),
    FISICA ("Física"),
    QUIMICA ("Química"),
    ARQUITETURA ("Arquitetura"),
    BIOLOGIA ("Biologia");

    private String materiaLivro;

    Materia(String materiaLivro){
        this.materiaLivro=materiaLivro;
    }

    @JsonCreator
    public static Materia fromString(String text){
        for(Materia materia : Materia.values()) {
            if(materia.materiaLivro.equalsIgnoreCase(text)) {
                return materia;
            }
        }
        throw new IllegalArgumentException("Nenhuma matéria encontrada para: " + text);
    }

    @JsonValue
    public String getMateriaLivro(){
        return materiaLivro;
    }
}
