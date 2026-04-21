package com.example.datagripjson

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter

class JsonViewerPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val textArea = JTextArea()
    private val searchField = SearchTextField(false)
    private val showCommentsCheckBox = JCheckBox("Show Comments", true)
    private val highlighterPainter = DefaultHighlighter.DefaultHighlightPainter(JBColor(0xFFF59D, 0x665C00))
    private val matchOffsets = mutableListOf<IntRange>()
    private var currentData: RowExtractedData? = null

    init {
        border = JBUI.Borders.empty(8)

        textArea.isEditable = false
        textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 14)
        textArea.lineWrap = false
        textArea.text = "Select one row in the result grid to view JSON."

        add(buildToolbar(), BorderLayout.NORTH)
        add(JScrollPane(textArea), BorderLayout.CENTER)
    }

    fun setRowData(data: RowExtractedData) {
        currentData = data
        renderCurrentData()
    }

    private fun renderCurrentData() {
        val data = currentData ?: return
        val rendered = JsonFormatter.format(
            rowData = data.values,
            comments = data.comments,
            showComments = showCommentsCheckBox.isSelected
        )
        textArea.text = rendered
        textArea.caretPosition = 0
        refreshSearch()
    }

    private fun buildToolbar(): JPanel {
        val toolbar = JPanel(BorderLayout())
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0))

        val copyButton = createToolbarIconButton(
            icon = AllIcons.Actions.Copy,
            toolTip = "Copy JSON"
        ) {
            CopyPasteManager.getInstance().setContents(StringSelection(textArea.text))
            notifyInfo("JSON copied")
        }

        val fieldHeight = copyButton.preferredSize.height
        searchField.toolTipText = "Search in JSON text"
        searchField.textEditor.preferredSize = Dimension(150, fieldHeight)
        searchField.textEditor.minimumSize = Dimension(110, fieldHeight)
        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = refreshSearch()
            override fun removeUpdate(e: DocumentEvent?) = refreshSearch()
            override fun changedUpdate(e: DocumentEvent?) = refreshSearch()
        })

        showCommentsCheckBox.isOpaque = false
        showCommentsCheckBox.toolTipText = "Toggle field comments"
        showCommentsCheckBox.addActionListener {
            renderCurrentData()
        }

        actions.add(copyButton)
        actions.add(searchField)
        actions.add(showCommentsCheckBox)

        toolbar.add(actions, BorderLayout.WEST)
        return toolbar
    }

    private fun refreshSearch() {
        val query = searchField.text.trim()
        val content = textArea.text

        textArea.highlighter.removeAllHighlights()
        matchOffsets.clear()

        if (query.isBlank() || content.isEmpty()) {
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

        if (matchOffsets.isNotEmpty()) {
            revealMatch(0)
        }
    }

    private fun revealMatch(index: Int) {
        if (index !in matchOffsets.indices) {
            return
        }

        val range = matchOffsets[index]
        textArea.caretPosition = range.first
        textArea.select(range.first, range.last + 1)
    }

    private fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Row Json Viewer")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun createToolbarIconButton(
        icon: Icon,
        toolTip: String,
        action: () -> Unit
    ): JButton {
        val button = JButton(icon)
        button.toolTipText = toolTip
        button.isFocusable = false
        button.border = JBUI.Borders.empty()
        button.isBorderPainted = false
        button.isContentAreaFilled = false
        button.isOpaque = false
        button.margin = JBUI.emptyInsets()
        button.addActionListener { action() }
        return button
    }
}
