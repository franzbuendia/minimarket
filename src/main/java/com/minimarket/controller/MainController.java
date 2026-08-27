package com.minimarket.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label lblTitulo;

    @FXML
    private void initialize() {
        lblTitulo.setText("Sistema POS - Minimarket");
    }
}
