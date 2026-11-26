package com.aidsa.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * JavaFX desktop application shell for AI DSA Mentor.
 * Orchestrates single-instance backend startup, launches the local web frontend in the
 * system's default browser (validated fallback for JavaFX 21 WebView limitations),
 * and performs clean shutdown on exit.
 */
public class DesktopApplication extends Application {

    private final BackendLauncher backendLauncher = new BackendLauncher();
    private final BorderPane rootPane = new BorderPane();

    private volatile String activeFrontendUrl;
    private WebViewManager webViewManager;

    @Override
    public void start(Stage primaryStage) {
        rootPane.setStyle("-fx-background-color: #10131a;");
        rootPane.setCenter(buildStatusPane(
                "Initializing AI DSA Mentor...",
                "Starting or attaching to single-instance local backend on 127.0.0.1:" + backendLauncher.getPort()
        ));

        Scene scene = new Scene(rootPane, 980, 640);
        primaryStage.setTitle("AI DSA Mentor — Desktop Shell");
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> shutdownAndExit());
        primaryStage.show();

        startBackendAndOpenUi();
    }

    @Override
    public void stop() {
        backendLauncher.shutdownIfOwned();
    }

    private void startBackendAndOpenUi() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return backendLauncher.ensureRunning(Duration.ofSeconds(25));
            } catch (Exception ex) {
                return new BackendLauncher.LaunchResult(false, false, backendLauncher.getBaseUrl(), ex.getMessage());
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            if (!result.ready()) {
                rootPane.setCenter(buildStatusPane("Backend Startup Failed", result.message()));
                return;
            }

            activeFrontendUrl = resolveFrontendUrl(result.baseUrl());
            boolean preferWebView = Boolean.getBoolean("aidsa.desktop.preferWebView");
            if (!preferWebView) {
                activateBrowserFallback(
                        activeFrontendUrl,
                        result.message() + "\nLocal Web UI opened in your default browser (validated fallback for JavaFX WebView)."
                );
                return;
            }

            try {
                webViewManager = new WebViewManager();
                rootPane.setTop(buildTopBar(activeFrontendUrl, result.message()));
                rootPane.setCenter(webViewManager.getWebView());
                webViewManager.loadAndValidate(activeFrontendUrl, outcome -> Platform.runLater(() -> {
                    if (!outcome.compatible()) {
                        activateBrowserFallback(
                                activeFrontendUrl,
                                "JavaFX WebView validation triggered browser fallback: " + outcome.details()
                        );
                    }
                }));
            } catch (Throwable ex) {
                activateBrowserFallback(activeFrontendUrl, "WebView unavailable: " + ex.getMessage());
            }
        }));
    }

    private void activateBrowserFallback(String url, String reason) {
        openInSystemBrowser(url);
        rootPane.setTop(null);

        VBox panel = buildStatusPane("AI DSA Mentor Desktop Ready", reason);
        Label infoLabel = new Label("Local URL: " + url + "  •  Model: Qwen2.5-Coder-3B-Instruct Q4_K_M (Manage via Local Web UI)");
        infoLabel.setStyle("-fx-text-fill: #7bd0ff; -fx-font-family: monospace; -fx-font-size: 12px;");

        Button openButton = new Button("Open in Default Browser");
        openButton.setStyle("-fx-background-color: #d0bcff; -fx-text-fill: #3c0091; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 6;");
        openButton.setOnAction(e -> openInSystemBrowser(url));

        Button quitButton = new Button("Quit & Stop Backend");
        quitButton.setStyle("-fx-background-color: #272a32; -fx-text-fill: #e1e2ec; -fx-padding: 9 18; -fx-background-radius: 6;");
        quitButton.setOnAction(e -> shutdownAndExit());

        HBox actions = new HBox(12, openButton, quitButton);
        actions.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(infoLabel, actions);
        rootPane.setCenter(panel);
    }

    private void openInSystemBrowser(String url) {
        try {
            getHostServices().showDocument(url);
        } catch (Exception ex) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(URI.create(url));
                } catch (Exception ignored) {
                    // URL remains visible in status panel
                }
            }
        }
    }

    private HBox buildTopBar(String url, String backendInfo) {
        Label badge = new Label("AI DSA Mentor • " + backendInfo);
        badge.setStyle("-fx-text-fill: #cbc3d7; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button browserBtn = new Button("Open in Browser");
        browserBtn.setStyle("-fx-background-color: #272a32; -fx-text-fill: #7bd0ff; -fx-font-size: 12px; -fx-padding: 4 10; -fx-background-radius: 4;");
        browserBtn.setOnAction(e -> openInSystemBrowser(url));

        HBox bar = new HBox(10, badge, spacer, browserBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setStyle("-fx-background-color: #0b0e15; -fx-border-color: #272a32; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private VBox buildStatusPane(String title, String details) {
        Label statusLabel = new Label(title);
        statusLabel.setStyle("-fx-text-fill: #e1e2ec; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label detailLabel = new Label(details == null ? "" : details);
        detailLabel.setStyle("-fx-text-fill: #cbc3d7; -fx-font-size: 13px;");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(680);

        VBox box = new VBox(14, statusLabel, detailLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36));
        box.setStyle("-fx-background-color: #10131a;");
        return box;
    }

    private String resolveFrontendUrl(String backendBaseUrl) {
        String configured = System.getProperty("aidsa.frontend.url", System.getenv("AIDSA_FRONTEND_URL"));
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return backendBaseUrl;
    }

    private void shutdownAndExit() {
        backendLauncher.shutdownIfOwned();
        Platform.exit();
    }
}
