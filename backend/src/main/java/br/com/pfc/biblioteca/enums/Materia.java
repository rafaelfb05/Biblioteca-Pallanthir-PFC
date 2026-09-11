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
            // Aceita tanto o rotulo ("Veterinaria") quanto o nome da constante ("VETERINARIA"),
            // assim o front-end pode usar sempre o nome do enum.
            if(materia.materiaLivro.equalsIgnoreCase(text) || materia.name().equalsIgnoreCase(text)) {
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
