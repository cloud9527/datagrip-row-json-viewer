package com.example.datagripjson

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class ShowCurrentRowAsJsonAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val state = RowJsonViewState.getInstance(project)
        val currentGrid = CurrentRowExtractor.findDataGrid(event)
        if (currentGrid != null) {
            state.updateActiveGrid(currentGrid)
        }

        val rowData = CurrentRowExtractor.extractFromGrid(state.lastActiveGrid)
            ?: CurrentRowExtractor.extractFromAction(event)

        if (rowData == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Row Json Viewer")
                .createNotification("No selected row detected. Select one row in result grid first.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Row JSON")
        toolWindow?.show {
            state.panel?.setRowData(rowData)
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project != null
    }
}
