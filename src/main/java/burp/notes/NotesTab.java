package burp.notes;

import burp.api.montoya.MontoyaApi;
import burp.notes.ui.NotesPanel;

import java.awt.Component;

public class NotesTab {
    private final NotesPanel notesPanel;

    public NotesTab(MontoyaApi montoyaApi, NotesPanel notesPanel) {
        this.notesPanel = notesPanel;
    }

    public String getTabCaption() {
        return "Notes+";
    }

    public Component getUiComponent() {
        return notesPanel;
    }
}
