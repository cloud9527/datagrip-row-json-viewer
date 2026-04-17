package com.example.datagripjson

import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridUtil
import com.intellij.database.run.ui.DataAccessType
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.temporal.TemporalAccessor
import javax.swing.JTable
import javax.swing.SwingUtilities

object CurrentRowExtractor {
    fun findDataGrid(event: AnActionEvent): DataGrid? = GridUtil.getDataGrid(event.dataContext)

    fun findDataGrid(component: Component?): DataGrid? {
        if (component == null) {
            return null
        }
        val dataContext = DataManager.getInstance().getDataContext(component)
        return GridUtil.getDataGrid(dataContext)
    }

    fun extractFromGrid(dataGrid: DataGrid?): LinkedHashMap<String, Any?>? {
        if (dataGrid == null || !dataGrid.isReady) {
            return null
        }

        val selectionModel = dataGrid.selectionModel
        val selectedRow = selectionModel.selectedRow ?: run {
            val selectedRows = selectionModel.selectedRows
            if (selectedRows.size() > 0) selectedRows.first() else null
        } ?: return null

        val model = dataGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS)
        if (!model.isValidRowIdx(selectedRow)) {
            return null
        }

        val row = runCatching { model.getRow(selectedRow) }.getOrNull() ?: return null
        val columns = model.columns
        if (columns.isEmpty()) {
            return null
        }

        val result = LinkedHashMap<String, Any?>(columns.size)
        for (column in columns) {
            val rawValue = runCatching { column.getValue(row) }.getOrNull()
            result[column.name] = normalizeValue(rawValue)
        }
        return result
    }

    fun extractFromAction(event: AnActionEvent): LinkedHashMap<String, Any?>? {
        extractFromGrid(findDataGrid(event))?.let { return it }
        val contextComponent = event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT)
        return extractFromComponentOrFocus(contextComponent)
    }

    fun extractFromCurrentFocus(): LinkedHashMap<String, Any?>? {
        return extractFromComponentOrFocus(null)
    }

    private fun extractFromComponentOrFocus(component: Component?): LinkedHashMap<String, Any?>? {
        val table = findTable(component)
            ?: findTable(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner)
            ?: return null

        val selectedViewRow = table.selectedRow
        if (selectedViewRow < 0) {
            return null
        }

        val modelRow = try {
            table.convertRowIndexToModel(selectedViewRow)
        } catch (_: Exception) {
            selectedViewRow
        }

        val model = table.model
        val result = LinkedHashMap<String, Any?>(model.columnCount)
        for (column in 0 until model.columnCount) {
            val columnName = model.getColumnName(column)
            val rawValue = runCatching { model.getValueAt(modelRow, column) }.getOrNull()
            result[columnName] = normalizeValue(rawValue)
        }
        return result
    }

    private fun findTable(component: Component?): JTable? {
        if (component == null) {
            return null
        }
        if (component is JTable) {
            return component
        }
        return SwingUtilities.getAncestorOfClass(JTable::class.java, component) as? JTable
    }

    private fun normalizeValue(value: Any?): Any? {
        return when (value) {
            null -> null
            is ByteArray -> "<BINARY:${value.size} bytes>"
            is Timestamp, is Date, is Time -> value.toString()
            is TemporalAccessor -> value.toString()
            else -> value
        }
    }
}
