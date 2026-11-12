package com.bcopstein.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/** Moldura comum: barra superior + menu lateral com as telas. */
public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 titulo = new H1("Gestão de eventos");
        titulo.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        addToNavbar(new DrawerToggle(), titulo);

        SideNav menu = new SideNav();
        menu.addItem(new SideNavItem("Eventos", EventosView.class, VaadinIcon.CALENDAR.create()));
        menu.addItem(new SideNavItem("Cadastrar evento", NovoEventoView.class, VaadinIcon.PLUS.create()));
        menu.addItem(new SideNavItem("Nova venda", NovaVendaView.class, VaadinIcon.CART.create()));
        menu.addItem(new SideNavItem("Vendas", VendasView.class, VaadinIcon.TICKET.create()));
        menu.addItem(new SideNavItem("Relatórios", RelatoriosView.class, VaadinIcon.CHART.create()));
        addToDrawer(new Scroller(menu));
        setPrimarySection(Section.DRAWER);
    }
}
