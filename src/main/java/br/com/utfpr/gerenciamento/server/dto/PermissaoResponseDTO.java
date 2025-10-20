package br.com.utfpr.gerenciamento.server.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class PermissaoResponseDTO {


    private Long id;

    private String nome;

}
