package br.com.pfc.biblioteca.controller;

import br.com.pfc.biblioteca.dto.dashboard.DashboardDTO;
import br.com.pfc.biblioteca.dto.dashboard.VendasDTO;
import br.com.pfc.biblioteca.service.DashboardService;
import br.com.pfc.biblioteca.service.VendasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final VendasService vendasService;

    public DashboardController(DashboardService service, VendasService vendasService) {
        this.service = service;
        this.vendasService = vendasService;
    }

    @GetMapping("/vendas")
    public VendasDTO vendas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return vendasService.montar(de, ate);
    }

    @GetMapping
    public DashboardDTO obter(){
        return service.montar();
    }
}
