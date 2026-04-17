package com.example.datagripjson

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter

class JsonViewerPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val textArea = JTextArea()
    private val searchField = JTextField()
    private val resultLabel = JLabel("No matches")
    private val highlighterPainter = DefaultHighlighter.DefaultHighlightPainter(JBColor(0xFFF59D, 0x665C00))

    private val matchOffsets = mutableListOf<IntRange>()
    private var currentMatchIndex = -1

    init {
        border = JBUI.Borders.empty(8)

        textArea.isEditable = false
        textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        textArea.lineWrap = false
        textArea.text = "Click 'Refresh Row' after selecting one row in the result grid."

        add(buildToolbar(), BorderLayout.NORTH)
        add(JScrollPane(textArea), BorderLayout.CENTER)
    }

    fun setJson(json: String) {
        textArea.text = json
        textArea.caretPosition = 0
        refreshSearch()
    }

    private fun buildToolbar(): JPanel {
        val toolbar = JPanel(BorderLayout(8, 0))

        val actions = JPanel()
        val refreshButton = JButton("Refresh Row")
        refreshButton.addActionListener {
            val state = RowJsonViewState.getInstance(project)
            val trackedGrid = CurrentRowExtractor.findDataGrid(state.trackedFocusComponent())
            if (trackedGrid != null) {
                state.lastActiveGrid = trackedGrid
            }
            val rowData = CurrentRowExtractor.extractFromGrid(state.lastActiveGrid)
                ?: CurrentRowExtractor.extractFromCurrentFocus()
            if (rowData == null) {
                notifyWarning("No selected row detected. Select a row first.")
                return@addActionListener
            }
            setJson(JsonFormatter.format(rowData))
        }

        val copyButton = JButton("Copy JSON")
        copyButton.addActionListener {
            CopyPasteManager.getInstance().setContents(StringSelection(textArea.text))
            notifyInfo("JSON copied")
        }

        actions.add(refreshButton)
        actions.add(copyButton)

        val searchPanel = JPanel(BorderLayout(6, 0))
        val prevButton = JButton("Prev")
        val nextButton = JButton("Next")

        prevButton.addActionListener { jumpToMatch(-1) }
        nextButton.addActionListener { jumpToMatch(1) }

        searchField.toolTipText = "Search in JSON text"
        val fieldHeight = copyButton.preferredSize.height
        searchField.preferredSize = Dimension(220, fieldHeight)
        searchField.minimumSize = Dimension(120, fieldHeight)
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = refreshSearch()
            override fun removeUpdate(e: DocumentEvent?) = refreshSearch()
            override fun changedUpdate(e: DocumentEvent?) = refreshSearch()
        })

        searchPanel.add(searchField, BorderLayout.CENTER)
        val nav = JPanel()
        nav.add(prevButton)
        nav.add(nextButton)
        nav.add(resultLabel)
        searchPanel.add(nav, BorderLayout.EAST)

        toolbar.add(actions, BorderLayout.WEST)
        toolbar.add(searchPanel, BorderLayout.CENTER)
        return toolbar
    }

    private fun refreshSearch() {
        val query = searchField.text
        val content = textArea.text

        textArea.highlighter.removeAllHighlights()
        matchOffsets.clear()
        currentMatchIndex = -1

        if (query.isBlank() || content.isEmpty()) {
            resultLabel.text = "No matches"
            return
        }

        val target = content.lowercase()
        val keyword = query.lowercase()

        var start = 0
        while (start < target.length) {
            val index = target.indexOf(keyword, start)
            if (index < 0) {
                break
            }
            val range = index until (index + keyword.length)
            matchOffsets.add(range)
            textArea.highlighter.addHighlight(range.first, range.last + 1, highlighterPainter)
            start = index + keyword.length
        }

        if (matchOffsets.isEmpty()) {
            resultLabel.text = "0 matches"
            return
        }

        currentMatchIndex = 0
        revealMatch(currentMatchIndex)
    }

    private fun jumpToMatch(direction: Int) {
        if (matchOffsets.isEmpty()) {
            return
        }

        currentMatchIndex = if (direction > 0) {
            (currentMatchIndex + 1) % matchOffsets.size
        } else {
            (currentMatchIndex - 1 + matchOffsets.size) % matchOffsets.size
        }
        revealMatch(currentMatchIndex)
    }

    private fun revealMatch(index: Int) {
        if (index !in matchOffsets.indices) {
            return
        }

        val range = matchOffsets[index]
        textArea.caretPosition = range.first
        textArea.select(range.first, range.last + 1)
        resultLabel.text = "${index + 1}/${matchOffsets.size}"
    }

    private fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Row Json Viewer")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyWarning(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Row Json Viewer")
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }
}
