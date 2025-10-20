package br.com.utfpr.gerenciamento.server.dto;

import br.com.utfpr.gerenciamento.server.model.RelatorioParams;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

import java.util.List;

public class RelatorioResponseDTO {
    private Long id;

    private String nome;

    private String nameReport;

    private List<RelatorioParams> paramsList;
}
