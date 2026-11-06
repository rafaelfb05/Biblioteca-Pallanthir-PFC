package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.dashboard.DashboardDTO;
import br.com.pfc.biblioteca.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardDTO obter(){
        return service.montar();
    }
}
