package burp.notes.model;

public class Note {
    private String title;
    private String markdownContent;

    public Note(String title, String markdownContent) {
        this.title = title;
        this.markdownContent = markdownContent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    @Override
    public String toString() {
        return title; // For display in JList or ComboBox
    }
}
