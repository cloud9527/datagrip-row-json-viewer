package com.example.datagripjson

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class RowJsonToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JsonViewerPanel(project)
        val state = RowJsonViewState.getInstance(project)
        state.panel = panel

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)
        state.syncPanelWithCurrentSelection()
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
