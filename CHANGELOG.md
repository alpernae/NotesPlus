# Changelog

## [Unreleased] - 2025-05-17

### Added

- Initial version of Notes+ extension.
- **Real-time HTML Preview**: Added a pane to display the rendered HTML output
  of the Markdown content, updating as you type.

### Changed

- **Localized UI Redesign for Notes+ Panel**:
  - Updated control buttons (New, Save, Delete) to use icons and tooltips.
  - Improved padding and spacing in the main panel for a cleaner layout.
  - Enhanced the appearance of the note title field.
  - Added subtle borders to scroll panes.
  - Increased default width of the notes list panel slightly.
  - Added padding within the markdown editor.
  - (Note: Global Look and Feel change was reverted to keep Burp Suite's
    default theme intact for the rest of the application).
- **Markdown Editor**: Enabled visual line wrapping.

### Security

- **Markdown Rendering**:
  - Raw HTML tags are now suppressed in the HTML preview to prevent potential
    XSS or rendering issues.
  - Image rendering is disabled in the HTML preview. Image Markdown syntax
    will still be saved but not displayed as an image.
