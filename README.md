# Notes+ Burp Suite Extension

Notes+ is a Burp Suite extension designed to provide a seamless note-taking experience directly within Burp Suite. It allows security testers and developers to efficiently create, manage, and organize their findings, observations, and thoughts using Markdown, with a live HTML preview.

## Features

*   **Integrated Note Management**:
    *   **Create, Save, Load, Delete**: Full CRUD (Create, Read, Update, Delete) operations for notes.
    *   **Note Listing**: Displays all saved notes in a selectable list.
    *   **Persistent Storage**: Notes are saved as individual Markdown (`.md`) files in a dedicated directory (`~/.BurpSuite/NotesPlusExtension` by default), ensuring your data persists across Burp Suite sessions.
    *   **Title Management**: Each note has a title. Saving a new note or an "Untitled Note" will prompt for a title if not provided.

*   **Markdown Editor with Live Preview**:
    *   **Markdown Input**: A `JTextPane` allows for writing notes using Markdown syntax.
    *   **Real-time Syntax Highlighting**: The editor provides styling for common Markdown elements as you type:
        *   **Bold**: `**text**` or `__text__`
        *   **Italics**: `*text*` or `_text_`
        *   **Headings (H1-H3)**: `# H1`, `## H2`, `### H3`. Markdown markers (e.g., `#`, `*`) are styled with a light gray color to be less obtrusive.
    *   **Live HTML Preview**: A `JEditorPane` shows the rendered HTML output of your Markdown content, updating approximately 300ms after you stop typing.
    *   **Secure Preview**:
        *   Raw HTML tags within the Markdown are suppressed in the preview to prevent potential rendering issues or XSS within the preview pane.
        *   Markdown image syntax (`![]()`) is recognized, but images are intentionally not rendered in the preview.

*   **User-Friendly Interface**:
    *   **Dedicated Burp Tab**: Notes+ is accessible via a "Notes++" tab in the Burp Suite main window.
    *   **Intuitive Layout**:
        *   **Top Panel**: Contains the note title field and control buttons (`+` for New, `✓` for Save, `✗` for Delete). Buttons are sized to match the height of the title field for a consistent look.
        *   **Main Area**: A split pane divides the notes list (left) from the editor/preview area (right).
        *   **Editor/Preview Split**: The right-hand side is a vertical split pane with the Markdown editor at the top and the HTML preview at the bottom.
    *   **Tooltips**: Buttons have tooltips explaining their function.

## Installation

1.  **Prerequisites**:
    *   Java Development Kit (JDK) version 17 or higher (for compilation).
    *   Gradle (for building the project).

2.  **Build the Extension**:
    *   Clone this repository or download the source code.
    *   Open a terminal or command prompt and navigate to the root directory of the project (`/root/Desktop/Notes+`).
    *   Run the Gradle build command:
        *   On Linux/macOS: `./gradlew build`
        *   On Windows: `gradlew.bat build`
    *   After a successful build, the extension JAR file will be located in `build/libs/`. The filename will typically be `BurpNotesPlus-VERSION.jar` (e.g., `BurpNotesPlus-2025.4.jar`).

3.  **Load in Burp Suite**:
    *   Open Burp Suite.
    *   Go to the `Extensions` tab.
    *   Under `Installed`, click the `Add` button.
    *   In the "Load new extension" dialog:
        *   Set "Extension type" to "Java".
        *   Click "Select file..." and navigate to the `build/libs/` directory in your project, then select the `BurpNotesPlus-VERSION.jar` file.
    *   Click "Next". The extension should load, and a "Notes++" tab will appear in the Burp Suite main window. You should also see log messages from the extension in the `Output` pane of the `Extensions` tab.

## Usage

1.  **Accessing Notes+**: Click on the "Notes++" tab in Burp Suite.
2.  **Creating a New Note**:
    *   Click the `+` (New Note) button.
    *   The title field will reset to "Untitled Note", and the editor will be cleared.
3.  **Editing the Title**:
    *   Click into the text field at the top (displaying "Untitled Note" or the current note's title) and type your desired title.
4.  **Writing Notes**:
    *   Type your content in Markdown syntax in the "Markdown Editor" pane (top-right).
    *   Observe the rendered HTML in the "HTML Preview" pane (bottom-right) as you type.
5.  **Saving a Note**:
    *   Click the `✓` (Save Note) button.
    *   If the current title is "Untitled Note" or empty, a dialog will appear prompting you to enter a title.
    *   The note will be saved, and its title will appear in the notes list on the left. If it's a new note or a renamed note, the list will update.
6.  **Loading an Existing Note**:
    *   Select a note title from the list on the left side of the panel.
    *   The selected note's title and content will be loaded into the title field and Markdown editor, respectively.
7.  **Deleting a Note**:
    *   Select a note from the list on the left.
    *   Click the `✗` (Delete Note) button.
    *   A confirmation dialog will appear. Click "Yes" to delete the note.
    *   The note will be removed from the list and deleted from the filesystem.

## Notes Storage

*   Notes are stored as individual Markdown files (`.md`) in the following directory: `[User Home Directory]/.BurpSuite/NotesPlusExtension/`.
*   For example, on Linux, this would typically be `/home/your_username/.BurpSuite/NotesPlusExtension/`.
*   The filename for each note is derived from its title (sanitized to be filesystem-friendly).


## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
---

This README provides an overview of the Notes+ extension, its features, and how to install and use it.
