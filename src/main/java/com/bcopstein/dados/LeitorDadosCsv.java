package com.bcopstein.dados;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Exemplo didatico de leitura de arquivos CSV simples, separados por ponto e virgula.
 * Nao trata campos com delimitadores escapados ou entre aspas.
 * Nao implementa validacoes de dominio nem cadastra objetos no sistema.
 */
public final class LeitorDadosCsv {
    private LeitorDadosCsv() { }

    public record EventoCsv(String id, String titulo, String descricao,
                            LocalDate data, String local, int capacidade,
                            BigDecimal precoBase, java.time.LocalTime horarioInicio) { }

    public record ModalidadeEventoCsv(String eventoId,
                                     int descontoPromocionalPercentual,
                                     int limitePromocional) { }

    public record VendaCsv(String id, String eventoId, String comprador,
                           LocalDateTime dataHora) { }

    public record IngressoCsv(String id, String vendaId, String modalidade,
                             String participanteNome, String participanteCpf,
                             String justificativa) { }

    private static List<String[]> lerLinhas(Path arquivo, int colunas) throws IOException {
        try (var linhas = Files.lines(arquivo, StandardCharsets.UTF_8)) {
            return linhas.skip(1)
                    .filter(linha -> !linha.isBlank())
                    .map(linha -> linha.split(";", -1))
                    .peek(campos -> {
                        if (campos.length != colunas) {
                            throw new IllegalArgumentException(
                                    "Quantidade incorreta de colunas em " + arquivo);
                        }
                    })
                    .toList();
        }
    }

    public static List<EventoCsv> lerEventos(Path arquivo) throws IOException {
        return lerLinhas(arquivo, 8).stream()
                .map(c -> new EventoCsv(c[0], c[1], c[2],
                        LocalDate.parse(c[3]), c[4], Integer.parseInt(c[5]),
                        new BigDecimal(c[6]), java.time.LocalTime.parse(c[7])))
                .toList();
    }

    public static List<ModalidadeEventoCsv> lerModalidades(Path arquivo) throws IOException {
        return lerLinhas(arquivo, 3).stream()
                .map(c -> new ModalidadeEventoCsv(c[0],
                        Integer.parseInt(c[1]), Integer.parseInt(c[2])))
                .toList();
    }

    public static List<VendaCsv> lerVendas(Path arquivo) throws IOException {
        return lerLinhas(arquivo, 4).stream()
                .map(c -> new VendaCsv(c[0], c[1], c[2], LocalDateTime.parse(c[3])))
                .toList();
    }

    public static List<IngressoCsv> lerIngressos(Path arquivo) throws IOException {
        return lerLinhas(arquivo, 6).stream()
                .map(c -> new IngressoCsv(c[0], c[1], c[2], c[3], c[4], c[5]))
                .toList();
    }
}
