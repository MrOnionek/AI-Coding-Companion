package dev.onion.aicoding.ui;

import dev.onion.aicoding.settings.Settings;
import dev.onion.aicoding.settings.SettingsManager;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SettingsWindow {

    private final Settings settings;
    private final SettingsManager settingsManager;
    private final List<String> providerNames;
    private final Runnable onApplied;

    public SettingsWindow(Settings settings, SettingsManager settingsManager,
                          List<String> providerNames, Runnable onApplied) {
        this.settings = settings;
        this.settingsManager = settingsManager;
        this.providerNames = providerNames;
        this.onApplied = onApplied;
    }

    public void show(Window owner) {
        Stage window = new Stage();
        window.initOwner(owner);
        window.initModality(Modality.WINDOW_MODAL);
        window.setTitle("Settings");

        PasswordField apiKey = new PasswordField();
        apiKey.setText(settings.getOpenAIApiKeyOverride());
        apiKey.setPromptText("Blank uses OPENAI_API_KEY");
        ComboBox<String> provider = new ComboBox<>(
                FXCollections.observableArrayList(providerNames));
        provider.setValue(settings.getDefaultAIProvider());
        CheckBox automaticReviews = new CheckBox();
        automaticReviews.setSelected(settings.isAutomaticReviewsEnabled());
        Spinner<Integer> debounce = new Spinner<>(250, 30_000,
                (int) settings.getReviewDebounceMillis(), 250);
        debounce.setEditable(true);
        Spinner<Integer> timeout = new Spinner<>(5, 900,
                settings.getReviewTimeoutSeconds(), 5);
        timeout.setEditable(true);
        Spinner<Integer> fontSize = new Spinner<>(8, 32,
                settings.getUiFontSize(), 1);
        fontSize.setEditable(true);
        ComboBox<Settings.Theme> theme = new ComboBox<>(
                FXCollections.observableArrayList(Settings.Theme.values()));
        theme.setValue(settings.getTheme());

        GridPane form = new GridPane();
        form.setPadding(new Insets(16));
        form.setHgap(12);
        form.setVgap(10);
        addRow(form, 0, "OpenAI API key override", apiKey);
        addRow(form, 1, "Default AI provider", provider);
        addRow(form, 2, "Automatic reviews", automaticReviews);
        addRow(form, 3, "Review debounce (ms)", debounce);
        addRow(form, 4, "Review timeout (seconds)", timeout);
        addRow(form, 5, "UI font size", fontSize);
        addRow(form, 6, "Theme", theme);

        Button save = new Button("Save");
        Button cancel = new Button("Cancel");
        save.setDefaultButton(true);
        cancel.setCancelButton(true);
        save.setOnAction(event -> {
            settings.setOpenAIApiKeyOverride(apiKey.getText());
            settings.setDefaultAIProvider(provider.getValue());
            settings.setAutomaticReviewsEnabled(automaticReviews.isSelected());
            settings.setReviewDebounceMillis(debounce.getValue());
            settings.setReviewTimeoutSeconds(timeout.getValue());
            settings.setUiFontSize(fontSize.getValue());
            settings.setTheme(theme.getValue());
            settingsManager.save(settings);
            onApplied.run();
            window.close();
        });
        cancel.setOnAction(event -> window.close());
        form.add(new HBox(8, save, cancel), 1, 7);

        window.setScene(new Scene(form, 460, 390));
        window.show();
    }

    private void addRow(GridPane form, int row, String label,
                        javafx.scene.Node control) {
        form.add(new Label(label), 0, row);
        form.add(control, 1, row);
    }
}
