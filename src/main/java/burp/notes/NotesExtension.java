package burp.notes;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.notes.ui.NotesPanel;

public class NotesExtension implements BurpExtension {
    private MontoyaApi montoyaApi;
    private Logging logging;

    @Override
    public void initialize(MontoyaApi api) {
        this.montoyaApi = api;
        this.logging = api.logging();

        logging.logToOutput("Notes+ Extension Initializing...");

        NotesPanel notesPanel = new NotesPanel(montoyaApi);
        NotesTab notesTab = new NotesTab(montoyaApi, notesPanel);

        montoyaApi.userInterface().registerSuiteTab("NotesPlus", notesTab.getUiComponent());

        logging.logToOutput(
                "Notes+ Extension Loaded Successfully.\nVersion: v2025.1.3\nAuthor: ALPEREN ERGEL (@alpernae)");
    }
}
