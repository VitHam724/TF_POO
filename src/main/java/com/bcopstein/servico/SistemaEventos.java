package com.bcopstein.servico;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Ingresso;
import com.bcopstein.dominio.Participante;
import com.bcopstein.dominio.RegraNegocioException;
import com.bcopstein.dominio.Venda;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ponto de entrada da logica de negocio (a interface Vaadin so conversa com esta
 * classe e com ConsultasGerenciais). Guarda os dados em memoria.
 */
public class SistemaEventos {
    private final Map<String, Evento> eventos = new LinkedHashMap<>();
    private final Map<String, Venda> vendas = new LinkedHashMap<>();
    private final Set<String> idsIngressos = new HashSet<>();
    private final Clock relogio;

    public SistemaEventos(Clock relogio) {
        this.relogio = relogio;
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    // ---------- Eventos ----------

    public synchronized void cadastrarEvento(Evento evento) {
        if (eventos.containsKey(evento.getId())) {
            throw new RegraNegocioException("Já existe um evento com o identificador " + evento.getId() + ".");
        }
        eventos.put(evento.getId(), evento);
    }

    public Optional<Evento> buscarEvento(String id) {
        return Optional.ofNullable(eventos.get(id));
    }

    public List<Evento> listarEventos() {
        return List.copyOf(eventos.values());
    }

    // ---------- Vendas ----------

    /** Venda feita pela interface: id gerado e data/hora = agora. */
    public Venda registrarVenda(String eventoId, String comprador, List<PedidoIngresso> pedidos) {
        return registrarVenda(null, eventoId, comprador, agora(), pedidos);
    }

    /**
     * Registra uma venda completa. Ordem: (1) monta e valida TODOS os objetos,
     * (2) o evento valida capacidade, cota e data, (3) so entao grava.
     * Qualquer falha lanca RegraNegocioException e nada e gravado.
     */
    public synchronized Venda registrarVenda(String idVenda, String eventoId, String comprador,
                                             LocalDateTime dataHora, List<PedidoIngresso> pedidos) {
        Evento evento = buscarEvento(eventoId)
                .orElseThrow(() -> new RegraNegocioException("Evento não encontrado: " + eventoId));

        String id = (idVenda == null) ? proximoIdVenda() : idVenda;
        if (vendas.containsKey(id)) {
            throw new RegraNegocioException("Já existe uma venda com o identificador " + id + ".");
        }
        if (pedidos == null || pedidos.isEmpty()) {
            throw new RegraNegocioException("Adicione pelo menos um ingresso à venda.");
        }

        List<Ingresso> ingressos = new ArrayList<>();
        Set<String> idsNestaVenda = new HashSet<>();
        int proximoNumero = proximoNumeroIngresso();
        for (int i = 0; i < pedidos.size(); i++) {
            PedidoIngresso pedido = pedidos.get(i);
            String idIngresso = pedido.id() != null ? pedido.id() : String.format("I%04d", proximoNumero++);
            if (idsIngressos.contains(idIngresso) || !idsNestaVenda.add(idIngresso)) {
                throw new RegraNegocioException("Identificador de ingresso repetido: " + idIngresso);
            }
            try {
                if (pedido.modalidade() == null) {
                    throw new RegraNegocioException("Escolha a modalidade.");
                }
                Participante participante = new Participante(pedido.nome(), pedido.cpf());
                ingressos.add(pedido.modalidade().criarIngresso(idIngresso, evento, participante, pedido.justificativa()));
            } catch (RegraNegocioException e) {
                throw new RegraNegocioException("Ingresso " + (i + 1) + ": " + e.getMessage());
            }
        }

        Venda venda = new Venda(id, evento, comprador, dataHora, ingressos); // valida CPF repetido etc.
        evento.registrarVenda(venda);                                        // valida capacidade, cota, data

        // Chegou aqui = tudo valido. So agora os dados passam a existir no sistema.
        vendas.put(id, venda);
        idsNestaVenda.forEach(idsIngressos::add);
        return venda;
    }

    public List<Venda> listarVendas() {
        return List.copyOf(vendas.values());
    }

    public List<Venda> listarVendasDoEvento(Evento evento) {
        return vendas.values().stream()
                .filter(v -> v.getEvento().equals(evento))
                .toList();
    }

    private String proximoIdVenda() {
        int n = vendas.size() + 1;
        while (vendas.containsKey(String.format("V%03d", n))) n++;
        return String.format("V%03d", n);
    }

    private int proximoNumeroIngresso() {
        int n = idsIngressos.size() + 1;
        while (idsIngressos.contains(String.format("I%04d", n))) n++;
        return n;
    }
}
