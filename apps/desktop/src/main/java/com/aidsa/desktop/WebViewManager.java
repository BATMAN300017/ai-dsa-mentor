package com.aidsa.desktop;

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Encapsulates JavaFX {@link WebView} setup and DOM/CSS/fetch compatibility verification.
 * Empirical validation on JavaFX 21 WebKit showed Prism D3D texture exceptions on heavy
 * backdrop-blur layers and WebKit HTTP2Loader fetch instability against localhost.
 * When incompatibility is detected, callers fall back to launching the local web app
 * in the system's default browser without modifying or degrading the supplied UI.
 */
public class WebViewManager {

    private final WebView webView;
    private final WebEngine webEngine;
    private final StringBuilder consoleErrors = new StringBuilder();

    public WebViewManager() {
        this.webView = new WebView();
        this.webEngine = this.webView.getEngine();
        this.webView.setStyle("-fx-background-color: #10131a;");
        this.webEngine.setJavaScriptEnabled(true);
        this.webEngine.setOnError(event -> {
            if (event.getMessage() != null) {
                consoleErrors.append(event.getMessage()).append('\n');
            }
        });
    }

    public WebView getWebView() {
        return webView;
    }

    public String getRecordedErrors() {
        return consoleErrors.toString();
    }

    /**
     * Loads the target URL, allows ES module scripts and health polling to settle,
     * and validates DOM mount, Tailwind dark styling, and API connectivity.
     */
    public void loadAndValidate(String url, Consumer<ValidationOutcome> onComplete) {
        consoleErrors.setLength(0);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                PauseTransition settleDelay = new PauseTransition(Duration.seconds(1.5));
                settleDelay.setOnFinished(event -> onComplete.accept(verifyRenderedFrontend()));
                settleDelay.play();
            } else if (newState == Worker.State.FAILED) {
                Throwable ex = webEngine.getLoadWorker().getException();
                String detail = ex != null ? ex.getMessage() : "WebView load worker failed";
                onComplete.accept(new ValidationOutcome(false, detail));
            }
        });

        webEngine.load(url);
    }

    private ValidationOutcome verifyRenderedFrontend() {
        try {
            Object raw = webEngine.executeScript("""
                    (function() {
                      var root = document.getElementById('root');
                      if (!root || root.children.length === 0) {
                        return 'EMPTY_ROOT';
                      }
                      var bodyStyle = window.getComputedStyle(document.body);
                      var bg = bodyStyle ? bodyStyle.backgroundColor : '';
                      var height = document.body ? document.body.scrollHeight : 0;
                      if (height < 400) {
                        return 'COLLAPSED_LAYOUT:' + height;
                      }
                      if (bg === 'rgba(0, 0, 0, 0)' || bg === 'rgb(255, 255, 255)') {
                        return 'CSS_NOT_APPLIED:' + bg;
                      }
                      var text = document.body.innerText || '';
                      if (text.indexOf('Backend: Connecting') !== -1) {
                        return 'FETCH_STALLED_CONNECTING';
                      }
                      return 'OK:' + height + ':' + bg;
                    })()
                    """);
            String result = raw == null ? "NULL_RESULT" : raw.toString();
            if (result.startsWith("OK:")) {
                return new ValidationOutcome(true, "WebView validated (" + result + ")");
            }
            return new ValidationOutcome(false, "WebView validation failed: " + result);
        } catch (Exception ex) {
            return new ValidationOutcome(false, "WebView JS probe error: " + ex.getMessage());
        }
    }

    public record ValidationOutcome(boolean compatible, String details) {
    }
}
