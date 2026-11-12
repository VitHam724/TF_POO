package com.bcopstein;

import com.bcopstein.dados.CarregadorDadosCsv;
import com.bcopstein.servico.ConsultasGerenciais;
import com.bcopstein.servico.SistemaEventos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Cria UMA instancia do sistema (dados em memoria) compartilhada por todas as telas
 * e faz a carga inicial dos CSV.
 */
@Configuration
public class ConfiguracaoSistema {

    @Bean
    public SistemaEventos sistemaEventos(@Value("${dados.pasta:dados}") String pasta) throws IOException {
        SistemaEventos sistema = new SistemaEventos(Clock.systemDefaultZone());
        CarregadorDadosCsv.carregar(sistema, Path.of(pasta));
        return sistema;
    }

    @Bean
    public ConsultasGerenciais consultasGerenciais(SistemaEventos sistema) {
        return new ConsultasGerenciais(sistema);
    }
}