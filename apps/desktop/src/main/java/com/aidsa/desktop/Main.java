package com.aidsa.desktop;

import javafx.application.Application;

/**
 * Non-Application entry point required for modular/jpackage JavaFX launching.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(DesktopApplication.class, args);
    }
}
