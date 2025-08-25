package burp.notes.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.notes.core.NoteManager;
import burp.notes.model.Note;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.html.HtmlWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class NotesPanel extends JPanel {
    private final Logging logging;
    private JTextPane markdownEditor;
    private JEditorPane htmlPreviewPane;
    private JList<String> notesList;
    private DefaultListModel<String> notesListModel;
    private JButton saveButton;
    private JButton newButton;
    private JButton deleteButton;
    private JTextField titleField;

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final NoteManager noteManager;
    private boolean isUpdating = false;
    private Timer renderDelayTimer;

    public NotesPanel(MontoyaApi montoyaApi) {
        this.logging = montoyaApi.logging();
        this.noteManager = new NoteManager(this.logging); // Pass the logging object
        setLayout(new BorderLayout());

        MutableDataSet options = new MutableDataSet();
        options.set(HtmlRenderer.SUPPRESS_HTML, true);

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options)
                .nodeRendererFactory(new NodeRendererFactory() {
                    @Override
                    public NodeRenderer apply(DataHolder options) {
                        return new NodeRenderer() {
                            @Override
                            public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                                Set<NodeRenderingHandler<?>> set = new HashSet<>();
                                set.add(new NodeRenderingHandler<>(Image.class,
                                        new NodeRenderingHandler.CustomNodeRenderer<Image>() {
                                            @Override
                                            public void render(Image node, NodeRendererContext context,
                                                    HtmlWriter html) {
                                                // Render nothing for images
                                            }
                                        }));
                                return set;
                            }
                        };
                    }
                })
                .build();

        initRenderTimer();
        initComponents();
        loadNotesList();
    }

    private void initRenderTimer() {
        renderDelayTimer = new Timer(300, e -> renderMarkdownAndPreview());
        renderDelayTimer.setRepeats(false);
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        titleField = new JTextField("Untitled Note");
        titleField.setFont(titleField.getFont().deriveFont(Font.BOLD, titleField.getFont().getSize() + 2f));
        titleField.setBorder(BorderFactory.createCompoundBorder(
                titleField.getBorder(),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        topPanel.add(titleField, BorderLayout.CENTER);

        // Get the preferred height of the title field to use for button sizing
        int buttonSize = titleField.getPreferredSize().height;
        Dimension squareButtonSize = new Dimension(buttonSize, buttonSize);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        newButton = new JButton("\u2795"); // Heavy Plus Sign for New
        newButton.setToolTipText("New Note");
        newButton.setPreferredSize(squareButtonSize);
        saveButton = new JButton("\u2714"); // Heavy Check Mark for Save
        saveButton.setToolTipText("Save Note");
        saveButton.setPreferredSize(squareButtonSize);
        deleteButton = new JButton("\u2716"); // Heavy Multiplication X for Delete
        deleteButton.setToolTipText("Delete Note");
        deleteButton.setPreferredSize(squareButtonSize);

        newButton.addActionListener(this::newNoteAction);
        saveButton.addActionListener(this::saveNoteAction);
        deleteButton.addActionListener(this::deleteNoteAction);

        controlPanel.add(newButton);
        controlPanel.add(saveButton);
        controlPanel.add(deleteButton);
        topPanel.add(controlPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.25);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        notesListModel = new DefaultListModel<>();
        notesList = new JList<>(notesListModel);
        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && notesList.getSelectedValue() != null) {
                loadSelectedNote();
            }
        });
        JScrollPane listScrollPane = new JScrollPane(notesList);
        listScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        mainSplitPane.setLeftComponent(listScrollPane);

        JSplitPane editorAndPreviewSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        editorAndPreviewSplitPane.setResizeWeight(0.5);

        markdownEditor = new JTextPane();
        markdownEditor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        markdownEditor.setMargin(new Insets(5, 8, 5, 8));
        markdownEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isUpdating && renderDelayTimer != null) {
                    renderDelayTimer.restart();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isUpdating && renderDelayTimer != null) {
                    renderDelayTimer.restart();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isUpdating && renderDelayTimer != null) {
                    renderDelayTimer.restart();
                }
            }
        });
        JScrollPane editorScrollPane = new JScrollPane(markdownEditor);
        editorScrollPane.setBorder(BorderFactory.createTitledBorder("Markdown Editor"));
        editorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editorAndPreviewSplitPane.setTopComponent(editorScrollPane);

        htmlPreviewPane = new JEditorPane();
        htmlPreviewPane.setContentType("text/html");
        htmlPreviewPane.setEditable(false);

        HTMLEditorKit kit = new HTMLEditorKit();
        htmlPreviewPane.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { word-wrap: break-word; }");
        styleSheet.addRule("p { word-wrap: break-word; }");

        JScrollPane previewScrollPane = new JScrollPane(htmlPreviewPane);
        previewScrollPane.setBorder(BorderFactory.createTitledBorder("HTML Preview"));
        editorAndPreviewSplitPane.setBottomComponent(previewScrollPane);

        mainSplitPane.setRightComponent(editorAndPreviewSplitPane);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private void renderMarkdownAndPreview() {
        if (isUpdating) {
            return;
        }
        isUpdating = true;

        try {
            String markdownText = markdownEditor.getText();
            StyledDocument doc = markdownEditor.getStyledDocument();
            int caretPosition = markdownEditor.getCaretPosition();

            SimpleAttributeSet defaultAttrs = new SimpleAttributeSet();
            Font editorFont = markdownEditor.getFont();
            StyleConstants.setFontFamily(defaultAttrs, editorFont.getFamily());
            StyleConstants.setFontSize(defaultAttrs, editorFont.getSize());
            StyleConstants.setBold(defaultAttrs, false);
            StyleConstants.setItalic(defaultAttrs, false);
            StyleConstants.setForeground(defaultAttrs, markdownEditor.getForeground());
            doc.setCharacterAttributes(0, doc.getLength(), defaultAttrs, true);

            com.vladsch.flexmark.util.ast.Node astRoot = parser.parse(markdownText);

            NodeVisitor visitor = new NodeVisitor(
                    new VisitHandler<>(StrongEmphasis.class, new Visitor<StrongEmphasis>() {
                        @Override
                        public void visit(StrongEmphasis node) {
                            SimpleAttributeSet boldContentAttrs = new SimpleAttributeSet();
                            boldContentAttrs.addAttributes(defaultAttrs);
                            StyleConstants.setBold(boldContentAttrs, true);

                            com.vladsch.flexmark.util.sequence.BasedSequence contentSequence = node.getText();
                            int contentStart = contentSequence.getStartOffset();
                            int contentLength = contentSequence.length();
                            if (contentStart >= 0 && (contentStart + contentLength) <= doc.getLength()) {
                                doc.setCharacterAttributes(contentStart, contentLength, boldContentAttrs, false);
                            }

                            SimpleAttributeSet hiddenMarkerAttrs = new SimpleAttributeSet();
                            StyleConstants.setFontFamily(hiddenMarkerAttrs, editorFont.getFamily());
                            StyleConstants.setFontSize(hiddenMarkerAttrs, editorFont.getSize());
                            StyleConstants.setForeground(hiddenMarkerAttrs, Color.LIGHT_GRAY);

                            if (node.getOpeningMarker().length() > 0) {
                                int markerStart = node.getOpeningMarker().getStartOffset();
                                int markerLength = node.getOpeningMarker().length();
                                if (markerStart >= 0 && (markerStart + markerLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(markerStart, markerLength, hiddenMarkerAttrs, false);
                                }
                            }
                            if (node.getClosingMarker().length() > 0) {
                                int markerStart = node.getClosingMarker().getStartOffset();
                                int markerLength = node.getClosingMarker().length();
                                if (markerStart >= 0 && (markerStart + markerLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(markerStart, markerLength, hiddenMarkerAttrs, false);
                                }
                            }
                        }
                    }),
                    new VisitHandler<>(Emphasis.class, new Visitor<Emphasis>() {
                        @Override
                        public void visit(Emphasis node) {
                            SimpleAttributeSet italicContentAttrs = new SimpleAttributeSet();
                            italicContentAttrs.addAttributes(defaultAttrs);
                            StyleConstants.setItalic(italicContentAttrs, true);

                            com.vladsch.flexmark.util.sequence.BasedSequence contentSequence = node.getText();
                            int contentStart = contentSequence.getStartOffset();
                            int contentLength = contentSequence.length();
                            if (contentStart >= 0 && (contentStart + contentLength) <= doc.getLength()) {
                                doc.setCharacterAttributes(contentStart, contentLength, italicContentAttrs, false);
                            }

                            SimpleAttributeSet hiddenMarkerAttrs = new SimpleAttributeSet();
                            StyleConstants.setFontFamily(hiddenMarkerAttrs, editorFont.getFamily());
                            StyleConstants.setFontSize(hiddenMarkerAttrs, editorFont.getSize());
                            StyleConstants.setForeground(hiddenMarkerAttrs, Color.LIGHT_GRAY);

                            if (node.getOpeningMarker().length() > 0) {
                                int markerStart = node.getOpeningMarker().getStartOffset();
                                int markerLength = node.getOpeningMarker().length();
                                if (markerStart >= 0 && (markerStart + markerLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(markerStart, markerLength, hiddenMarkerAttrs, false);
                                }
                            }
                            if (node.getClosingMarker().length() > 0) {
                                int markerStart = node.getClosingMarker().getStartOffset();
                                int markerLength = node.getClosingMarker().length();
                                if (markerStart >= 0 && (markerStart + markerLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(markerStart, markerLength, hiddenMarkerAttrs, false);
                                }
                            }
                        }
                    }),
                    new VisitHandler<>(Heading.class, new Visitor<Heading>() {
                        @Override
                        public void visit(Heading node) {
                            SimpleAttributeSet headingAttrs = new SimpleAttributeSet();
                            headingAttrs.addAttributes(defaultAttrs);
                            StyleConstants.setBold(headingAttrs, true);
                            int baseSize = editorFont.getSize();
                            int headingTextSize = baseSize;
                            switch (node.getLevel()) {
                                case 1:
                                    headingTextSize = baseSize + 6;
                                    break;
                                case 2:
                                    headingTextSize = baseSize + 4;
                                    break;
                                case 3:
                                    headingTextSize = baseSize + 2;
                                    break;
                                default:
                                    break;
                            }
                            StyleConstants.setFontSize(headingAttrs, headingTextSize);

                            com.vladsch.flexmark.util.sequence.BasedSequence contentSequence = node.getText();
                            if (contentSequence != null && contentSequence.length() > 0) {
                                int contentStart = contentSequence.getStartOffset();
                                int contentLength = contentSequence.length();
                                if (contentStart >= 0 && (contentStart + contentLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(contentStart, contentLength, headingAttrs, false);
                                }
                            }

                            SimpleAttributeSet hiddenMarkerAttrs = new SimpleAttributeSet();
                            StyleConstants.setFontFamily(hiddenMarkerAttrs, editorFont.getFamily());
                            StyleConstants.setFontSize(hiddenMarkerAttrs, headingTextSize);
                            StyleConstants.setForeground(hiddenMarkerAttrs, Color.LIGHT_GRAY);

                            if (node.getOpeningMarker().length() > 0) {
                                int markerStart = node.getOpeningMarker().getStartOffset();
                                int markerLength = node.getOpeningMarker().length();
                                if (markerStart >= 0 && (markerStart + markerLength) <= doc.getLength()) {
                                    doc.setCharacterAttributes(markerStart, markerLength, hiddenMarkerAttrs, false);
                                }
                            }
                        }
                    }));

            visitor.visit(astRoot);

            if (caretPosition <= doc.getLength()) {
                markdownEditor.setCaretPosition(caretPosition);
            }

            String htmlContent = renderer.render(astRoot);
            htmlPreviewPane.setText(htmlContent);
            htmlPreviewPane.setCaretPosition(0);

        } finally {
            isUpdating = false;
        }
    }

    private void newNoteAction(ActionEvent e) {
        clearEditor();
        titleField.setText("Untitled Note");
        notesList.clearSelection();
    }

    private void saveNoteAction(ActionEvent e) {
        String title = titleField.getText().trim();
        if (title.isEmpty() || title.equals("Untitled Note")) {
            String newTitle = JOptionPane.showInputDialog(this, "Enter note title:", "Save Note",
                    JOptionPane.PLAIN_MESSAGE);
            if (newTitle == null || newTitle.trim().isEmpty()) {
                logging.logToOutput("Save cancelled or title empty.");
                return;
            }
            title = newTitle.trim();
            titleField.setText(title);
        }

        final String finalTitle = title;
        String markdownContent = markdownEditor.getText();
        Note noteToSave = new Note(finalTitle, markdownContent);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                noteManager.saveNote(noteToSave);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    logging.logToOutput("Note saved: " + finalTitle);
                    if (!notesListModel.contains(finalTitle)) {
                        notesListModel.addElement(finalTitle);
                        notesList.setSelectedValue(finalTitle, true);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    logging.logToError("Error saving note '" + finalTitle + "': " + ex.getMessage());
                    JOptionPane.showMessageDialog(NotesPanel.this, "Error saving note: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadSelectedNote() {
        String selectedTitle = notesList.getSelectedValue();
        if (selectedTitle == null)
            return;

        new SwingWorker<Note, Void>() {
            @Override
            protected Note doInBackground() {
                return noteManager.loadNote(selectedTitle);
            }

            @Override
            protected void done() {
                try {
                    Note loadedNote = get();
                    if (loadedNote != null) {
                        if (renderDelayTimer != null) {
                            renderDelayTimer.stop();
                        }
                        isUpdating = true;
                        markdownEditor.setText(loadedNote.getMarkdownContent());
                        isUpdating = false;
                        renderMarkdownAndPreview();
                        titleField.setText(loadedNote.getTitle());
                        logging.logToOutput("Note loaded: " + loadedNote.getTitle());
                    } else {
                        logging.logToOutput("Failed to load note or note not found: " + selectedTitle);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    logging.logToError("Error loading note '" + selectedTitle + "': " + ex.getMessage());
                    JOptionPane.showMessageDialog(NotesPanel.this, "Error loading note: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteNoteAction(ActionEvent e) {
        String selectedTitle = notesList.getSelectedValue();
        if (selectedTitle == null) {
            JOptionPane.showMessageDialog(this, "Please select a note to delete.", "Delete Note",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + selectedTitle + "'?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            final String titleToDelete = selectedTitle;
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return noteManager.deleteNote(titleToDelete);
                }

                @Override
                protected void done() {
                    try {
                        boolean deleted = get();
                        if (deleted) {
                            logging.logToOutput("Note deleted: " + titleToDelete);
                            notesListModel.removeElement(titleToDelete);
                            if (titleToDelete.equals(titleField.getText())) {
                                clearEditor();
                            }
                            if (!notesListModel.isEmpty()) {
                                notesList.setSelectedIndex(0);
                            } else {
                                clearEditor();
                            }
                        } else {
                            logging.logToOutput("Note not found for deletion or error: " + titleToDelete);
                            JOptionPane.showMessageDialog(NotesPanel.this, "Could not delete note: " + titleToDelete,
                                    "Delete Error", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        logging.logToError("Error deleting note '" + titleToDelete + "': " + ex.getMessage());
                        JOptionPane.showMessageDialog(NotesPanel.this, "Error deleting note: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void loadNotesList() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return noteManager.getAllNoteTitles();
            }

            @Override
            protected void done() {
                try {
                    List<String> titles = get();
                    notesListModel.clear();
                    for (String title : titles) {
                        notesListModel.addElement(title);
                    }
                    if (!notesListModel.isEmpty()) {
                        notesList.setSelectedIndex(0);
                    } else {
                        clearEditor();
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    logging.logToError("Error loading note list: " + ex.getMessage());
                    JOptionPane.showMessageDialog(NotesPanel.this, "Error loading note list: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    clearEditor();
                }
            }
        }.execute();
    }

    private void clearEditor() {
        titleField.setText("Untitled Note");
        if (renderDelayTimer != null) {
            renderDelayTimer.stop();
        } else {
            if (logging != null) {
                logging.logToError(
                        "clearEditor: renderDelayTimer was unexpectedly null when trying to stop. This may indicate a build/deployment issue or an earlier initialization problem.");
            }
        }
        isUpdating = true;
        markdownEditor.setText("");
        htmlPreviewPane.setText("");
        isUpdating = false;
        renderMarkdownAndPreview();
    }

    /**
     * Cleanup method to be called when the extension is unloaded.
     * Stops and disposes of the renderDelayTimer to prevent memory leaks.
     */
    public void cleanup() {
        if (renderDelayTimer != null) {
            renderDelayTimer.stop();
            renderDelayTimer = null;
            if (logging != null) {
                logging.logToOutput("NotesPanel: renderDelayTimer cleaned up successfully.");
            }
        }
    }
}
