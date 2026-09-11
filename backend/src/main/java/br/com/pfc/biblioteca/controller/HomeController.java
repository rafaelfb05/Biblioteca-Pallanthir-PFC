package br.com.pfc.biblioteca.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "app", "Biblioteca Pallanthir",
                "description", "Back-end do sistema Pallanthir — biblioteca para estudantes",
                "version", "0.0.1-SNAPSHOT",
                "status", "online",
                "docs", "/swagger-ui.html",
                "github", "https://github.com/rafaelfb05/Biblioteca-Pallanthir-PFC",
                "team", "Projeto Final de Curso — UMC 2026"
        );
    }
}