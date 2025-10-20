package br.com.utfpr.gerenciamento.server.dto;

import br.com.utfpr.gerenciamento.server.config.LocalDateDeserializer;
import br.com.utfpr.gerenciamento.server.config.LocalDateSerializer;
import br.com.utfpr.gerenciamento.server.model.SaidaItem;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class SaidaResponseDTO {
    private Long id;

    private LocalDate dataSaida;

    private String observacao;

    private List<SaidaItem> saidaItem;

    private Usuario usuarioResponsavel;
    private Long idEmprestimo;
}
