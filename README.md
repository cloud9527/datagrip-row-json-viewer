# DataGrip Row JSON Viewer

A lightweight DataGrip plugin that displays the currently selected result-row as pretty-printed vertical JSON.

## What It Does

When you work in a DataGrip result grid, this plugin helps you inspect row data quickly in a readable JSON format.

### Core Features

- Show the current row as formatted JSON in a right-side Tool Window (`Row JSON`)
- Add action to DataGrip result-grid right-click menu (`Show Current Row As JSON`)
- Auto-update JSON when row selection changes (no manual refresh required)
- Copy current JSON content with one click
- Search inside JSON text with highlight and previous/next navigation

## Why This Plugin

DataGrid rows are often wide and hard to scan horizontally. This plugin converts one row into a vertical JSON view so field names and values are easier to read, compare, and copy.

## Installation (From ZIP)

1. Build the plugin ZIP:
   - Windows: `./gradlew.bat buildPlugin`
   - macOS/Linux: `./gradlew buildPlugin`
2. Find output under:
   - `build/distributions/`
3. In DataGrip:
   - `Settings` -> `Plugins` -> gear icon -> `Install Plugin from Disk...`
4. Choose the generated ZIP and restart DataGrip.

## Usage

### Option A: Right-click in result grid

1. Open query results in DataGrip.
2. Right-click a row in the result grid.
3. Click `Show Current Row As JSON`.
4. The `Row JSON` tool window opens and shows formatted JSON.

### Option B: Tools menu

- `Tools` -> `Show Current Row As JSON`

### In the `Row JSON` panel

- `Copy JSON`: copy all current JSON text
- Search box: type keyword to highlight matches
- `Prev` / `Next`: jump between matches

## Compatibility

- Target IDE: DataGrip 2025.1
- Plugin compatibility range: build `252.*`

## Build From Source

Requirements:

- JDK 17+
- Gradle wrapper included in this repository

Build command:

```bash
./gradlew buildPlugin
```

## Project Structure

- `src/main/resources/META-INF/plugin.xml`: plugin registration, actions, tool window
- `src/main/kotlin/com/example/datagripjson/CurrentRowExtractor.kt`: DataGrid row extraction
- `src/main/kotlin/com/example/datagripjson/RowJsonViewState.kt`: active grid tracking + selection listeners
- `src/main/kotlin/com/example/datagripjson/JsonViewerPanel.kt`: UI panel, search, copy
- `src/main/kotlin/com/example/datagripjson/ShowCurrentRowAsJsonAction.kt`: entry action from menu/context menu

## Development Notes

- Uses official DataGrip/JetBrains DataGrid APIs for selected-row access and live updates.
- Keeps JSON rendering logic simple and focused on row inspection.

## License

If you plan to publish this plugin publicly, add your preferred open-source license file (for example `MIT` or `Apache-2.0`).
