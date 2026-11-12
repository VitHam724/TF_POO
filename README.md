# Sistema de Gestão de Eventos e Ingressos — TF de POO

Sistema em Java com interface Vaadin para cadastrar eventos, vender ingressos em quatro
modalidades e consultar ocupação e arrecadação. Os dados ficam em memória e são carregados
dos CSV da pasta `dados/` a cada execução.

## Como executar

Pré-requisitos: JDK 21 ou mais novo (testado para rodar também no 25) e Maven 3.9+.

```bash
mvn spring-boot:run
```

Na primeira execução o Maven baixa as dependências e o Vaadin prepara o frontend (instala o
Node automaticamente, se preciso); isso pode levar alguns minutos. Depois, abra
http://localhost:8080 (o navegador costuma abrir sozinho).

O comando deve ser executado na raiz do projeto, porque os CSV são lidos de `dados/`
(configurável em `src/main/resources/application.properties`, chave `dados.pasta`).

Para testar só a lógica de negócio no terminal, sem a interface:

```bash
mvn compile exec:java -Dexec.mainClass=com.bcopstein.console.DemoConsole
```

## Organização do código

```
com.bcopstein
├── Application, ConfiguracaoSistema   inicialização (Spring Boot) e carga dos CSV
├── dominio                            regras de negócio, sem nenhuma dependência do Vaadin
│   ├── Ingresso (abstrata)            Inteira, MeiaEntrada, VIP, Promocional
│   ├── Modalidade (enum)              as 4 modalidades fixas + fábrica de ingressos
│   ├── Evento, Venda, Participante
│   └── RegraNegocioException
├── servico
│   ├── SistemaEventos                 cadastro de eventos e registro de vendas (validação)
│   ├── ConsultasGerenciais            consultas com Stream API
│   └── PedidoIngresso                 dados de entrada de um ingresso (tela/CSV)
├── dados                              LeitorDadosCsv (código-base) e CarregadorDadosCsv
├── console                            DemoConsole (demonstração em texto)
└── ui                                 telas Vaadin
```

A interface só chama `SistemaEventos` e `ConsultasGerenciais`. Nenhuma regra de negócio fica
nas telas: a tela de venda apenas monta a lista de pedidos e mostra a mensagem da exceção
quando a venda é recusada.

## Decisões de modelagem

**Hierarquia de ingressos.** `Ingresso` é abstrata e declara `calcularPreco()`,
`permiteEntradaAntecipada()` e `getModalidade()`. Cada subclasse implementa sua regra:
Inteira = 100%, MeiaEntrada = 50% (e guarda a justificativa), VIP = 150% (e é a única que
responde `true` para entrada antecipada), Promocional = preço-base menos o desconto do evento.
Há ainda `consomeCotaPromocional()`, com implementação padrão `false` sobrescrita apenas em
`Promocional`, para que a validação da cota também seja polimórfica (sem `instanceof`).

**Onde o tipo concreto é escolhido.** O único `switch` sobre modalidade está em
`Modalidade.criarIngresso(...)`, que funciona como fábrica: transforma a escolha feita na tela
ou o texto do CSV no objeto concreto. Depois de criado, nenhum código pergunta o tipo do
ingresso — preço, entrada antecipada e cota são resolvidos por polimorfismo.

**Evento.** Guarda suas configurações (inclusive desconto e limite promocionais) e a lista das
suas vendas. Não tem setters, o que garante a regra de que eventos não são editados.
O método `registrarVenda` valida data, capacidade e cota e só no final faz o `add`.

**Venda.** Recebe a lista completa de ingressos no construtor e valida: comprador
informado, pelo menos um ingresso, todos do mesmo evento e CPF sem repetição na venda.
A lista é armazenada como imutável (`List.copyOf`).

**Validação "tudo ou nada".** Em `SistemaEventos.registrarVenda` todos os objetos são criados
e validados primeiro; só depois que tudo passou a venda é gravada. Qualquer violação lança
`RegraNegocioException` e nada é registrado.

**Carga dos CSV.** `CarregadorDadosCsv` usa o leitor fornecido e registra eventos e vendas
pelos mesmos métodos usados na interface, então os dados iniciais passam pelas mesmas
validações.

**Data atual.** `SistemaEventos` recebe um `Clock`, usado para saber o que é futuro ou
realizado e para datar as vendas feitas pela interface.

## Regras implementadas

| Regra | Onde |
|---|---|
| Identificador de evento único | `SistemaEventos.cadastrarEvento` |
| Capacidade limita o total vendido | `Evento.registrarVenda` |
| Cota promocional por evento | `Evento.registrarVenda` + `Ingresso.consomeCotaPromocional` |
| Sem vendas para eventos realizados | `Evento.registrarVenda` (data da venda vs. início) |
| CPF com 11 dígitos, como texto | `Participante` |
| CPF não se repete na mesma venda | `Venda` (construtor) |
| Meia-entrada exige justificativa | `MeiaEntrada` (construtor) |
| Ids únicos de venda e de ingresso | `SistemaEventos.registrarVenda` |
| Venda validada como um todo | `SistemaEventos.registrarVenda` |

## Funcionalidades e consultas

| Item do enunciado | Tela | Método |
|---|---|---|
| Listar eventos e detalhes | Eventos | `listarEventos`, diálogo de detalhes |
| Cadastrar evento | Cadastrar evento | `SistemaEventos.cadastrarEvento` |
| Registrar venda / impedir inválidas | Nova venda | `SistemaEventos.registrarVenda` |
| Consultar vendas e ingressos | Vendas | `listarVendas`, `listarVendasDoEvento` |
| Disponibilidade | Eventos (detalhes), Nova venda | `Evento.getDisponibilidade`, `getDisponibilidadePromocional` |
| Eventos futuros e realizados | Eventos (filtro Situação) | `eventosFuturos`, `eventosRealizados` |
| Arrecadação e ocupação de um evento | Eventos (detalhes) | `Evento.getArrecadacao`, `getPercentualOcupacao` |
| Futuros ordenados por data | Relatórios › Eventos futuros | `eventosFuturos` |
| Filtro por período ou local | Eventos | `eventosFiltrados` |
| Ingressos vendidos por evento | Relatórios › Por evento | `ingressosVendidosPorEvento` (`groupingBy` + `summingInt`) |
| Arrecadação por evento | Relatórios › Por evento | `arrecadacaoPorEvento` (`groupingBy` + `reducing`) |
| Arrecadação por modalidade | Relatórios › Por modalidade | `arrecadacaoPorModalidade` |
| Ranking por ingressos vendidos | Relatórios › Ranking | `rankingPorIngressosVendidos` (`sorted`) |
| Percentual de ocupação | Relatórios › Por evento | `percentualOcupacaoPorEvento` |
| Modalidade mais vendida | Relatórios › Por evento | `modalidadesMaisVendidas` |
| Entrada antecipada por evento | Relatórios › Por evento | `entradaAntecipadaPorEvento` (`filter(Ingresso::permiteEntradaAntecipada)`) |

## Esclarecimentos de requisitos

O enunciado pede que dúvidas sejam levadas ao professor antes de assumir um comportamento.
As decisões abaixo foram adotadas provisoriamente e **devem ser confirmadas** (atualizar
esta seção com a resposta obtida):

1. **Venda para evento passado na carga dos CSV.** A venda é comparada com o início do
   evento usando a data/hora da própria venda. Assim, as vendas do CSV para o E004
   (feitas antes de 12/09) são aceitas, e novas vendas pela interface usam o momento atual.
2. **Momento em que o evento deixa de aceitar vendas.** Considerado o horário de início:
   a partir dele, o evento é "realizado".
3. **Cadastro de evento com data passada.** Permitido (fica disponível para consulta, mas não
   aceita vendas).
4. **Empate na modalidade mais vendida.** Todas as modalidades empatadas são exibidas.
5. **Arredondamento.** Preços com 2 casas decimais, `RoundingMode.HALF_UP`.
6. **Limite promocional.** Deve estar entre 0 e a capacidade do evento; desconto entre 0 e 100%.

## Roteiro da demonstração

1. **Carga:** abrir a tela Eventos e mostrar os 5 eventos vindos dos CSV (E004 aparece como
   realizado).
2. **Venda válida mista:** em Nova venda, escolher E002, adicionar uma inteira, uma meia
   (justificativa "Estudante"), um VIP e um promocional, e registrar. Conferir em Vendas.
3. **Vendas inválidas** (cada uma é recusada e nada é gravado):
   - E001 com um ingresso promocional — a cota de 25 já está esgotada;
   - E004 — evento já realizado;
   - dois ingressos com o mesmo CPF;
   - meia-entrada sem justificativa;
   - mais ingressos do que os lugares restantes (o E001 tem só 10).
4. **Relatórios:** mostrar as abas, destacando a coluna "Entrada antecipada".
5. **Polimorfismo:** abrir `ConsultasGerenciais.precosPorModalidade` e `Venda.getValorTotal`.
