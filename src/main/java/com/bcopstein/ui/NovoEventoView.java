package com.bcopstein.ui;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.RegraNegocioException;
import com.bcopstein.servico.SistemaEventos;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Funcionalidade 2: cadastrar evento, incluindo os parametros da modalidade promocional. */
@Route(value = "eventos/novo", layout = MainLayout.class)
@PageTitle("Cadastrar evento")
public class NovoEventoView extends VerticalLayout {

    private final TextField id = new TextField("Código");
    private final TextField titulo = new TextField("Título");
    private final TextArea descricao = new TextArea("Descrição");
    private final DatePicker data = new DatePicker("Data");
    private final TimePicker horario = new TimePicker("Horário de início");
    private final TextField local = new TextField("Local");
    private final IntegerField capacidade = new IntegerField("Capacidade máxima");
    private final BigDecimalField precoBase = new BigDecimalField("Preço-base (R$)");
    private final IntegerField desconto = new IntegerField("Desconto promocional (%)");
    private final IntegerField limitePromocional = new IntegerField("Limite de ingressos promocionais");

    public NovoEventoView(SistemaEventos sistema) {
        setMaxWidth("800px");
        id.setPlaceholder("Ex.: E006");
        data.setLocale(Formatos.BR);
        desconto.setMin(0);
        desconto.setMax(100);
        id.setRequiredIndicatorVisible(true);
        titulo.setRequiredIndicatorVisible(true);
        data.setRequiredIndicatorVisible(true);
        horario.setRequiredIndicatorVisible(true);
        local.setRequiredIndicatorVisible(true);
        capacidade.setRequiredIndicatorVisible(true);
        precoBase.setRequiredIndicatorVisible(true);
        desconto.setRequiredIndicatorVisible(true);
        limitePromocional.setRequiredIndicatorVisible(true);

        FormLayout form = new FormLayout(id, titulo, descricao, local, data, horario,
                capacidade, precoBase, desconto, limitePromocional);
        form.setColspan(descricao, 2);

        Button cadastrar = new Button("Cadastrar evento", e -> {
            try {
                if (capacidade.getValue() == null || desconto.getValue() == null
                        || limitePromocional.getValue() == null || precoBase.getValue() == null) {
                    throw new RegraNegocioException("Preencha capacidade, preço-base, desconto e limite promocional.");
                }
                Evento evento = new Evento(id.getValue(), titulo.getValue(), descricao.getValue(),
                        data.getValue(), horario.getValue(), local.getValue(), capacidade.getValue(),
                        precoBase.getValue(), desconto.getValue(), limitePromocional.getValue());
                sistema.cadastrarEvento(evento);
                Formatos.sucesso("Evento " + evento.getId() + " cadastrado.");
                getUI().ifPresent(ui -> ui.navigate(EventosView.class));
            } catch (RegraNegocioException ex) {
                Formatos.erro(ex.getMessage());
            }
        });
        cadastrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2("Cadastrar evento"), form, cadastrar);
    }
}
