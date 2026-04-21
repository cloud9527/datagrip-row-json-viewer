package com.example.datagripjson

import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.GridUtil
import com.intellij.database.datagrid.JdbcGridColumn
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.run.ui.DataAccessType
import com.intellij.database.util.DasUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Date as UtilDate
import javax.swing.JTable
import javax.swing.SwingUtilities

data class RowExtractedData(
    val values: LinkedHashMap<String, Any?>,
    val comments: Map<String, String>
)

object CurrentRowExtractor {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun findDataGrid(event: AnActionEvent): DataGrid? = GridUtil.getDataGrid(event.dataContext)

    fun findDataGrid(component: Component?): DataGrid? {
        if (component == null) {
            return null
        }
        val dataContext = DataManager.getInstance().getDataContext(component)
        return GridUtil.getDataGrid(dataContext)
    }

    fun extractFromGrid(dataGrid: DataGrid?): RowExtractedData? {
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
        val commentByColumn = resolveCommentsByColumn(dataGrid, columns)
        for (column in columns) {
            val rawValue = runCatching { column.getValue(row) }.getOrNull()
            result[column.name] = normalizeValue(rawValue)
        }
        return RowExtractedData(result, commentByColumn)
    }

    fun extractFromAction(event: AnActionEvent): RowExtractedData? {
        extractFromGrid(findDataGrid(event))?.let { return it }
        val contextComponent = event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT)
        return extractFromComponentOrFocus(contextComponent)
    }

    fun extractFromCurrentFocus(): RowExtractedData? {
        return extractFromComponentOrFocus(null)
    }

    private fun extractFromComponentOrFocus(component: Component?): RowExtractedData? {
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
        return RowExtractedData(result, emptyMap())
    }

    private fun resolveCommentsByColumn(
        dataGrid: DataGrid,
        columns: List<GridColumn>
    ): Map<String, String> {
        val project = dataGrid.project
        val dataSources = DbPsiFacade.getInstance(project).dataSources
        if (dataSources.isEmpty()) {
            return emptyMap()
        }

        val tableCommentCache = mutableMapOf<TableRef, Map<String, String>>()
        val comments = LinkedHashMap<String, String>()
        for (column in columns) {
            val jdbcColumn = column as? JdbcGridColumn ?: continue
            val tableRef = TableRef(
                catalog = jdbcColumn.catalog?.trim().orEmpty(),
                schema = jdbcColumn.schema?.trim().orEmpty(),
                table = jdbcColumn.table?.trim().orEmpty()
            )
            if (tableRef.table.isBlank()) {
                continue
            }

            val columnCommentMap = tableCommentCache.getOrPut(tableRef) {
                findTableColumnComments(dataSources, tableRef)
            }
            val comment = columnCommentMap[column.name]
                ?: columnCommentMap.entries.firstOrNull { it.key.equals(column.name, ignoreCase = true) }?.value
            if (!comment.isNullOrBlank()) {
                comments[column.name] = comment
            }
        }
        return comments
    }

    private fun findTableColumnComments(
        dataSources: List<com.intellij.database.psi.DbDataSource>,
        tableRef: TableRef
    ): Map<String, String> {
        for (dataSource in dataSources) {
            for (table in DasUtil.getTables(dataSource)) {
                if (!nameMatches(table.name, tableRef.table)) {
                    continue
                }
                val catalog = DasUtil.getCatalog(table).orEmpty()
                if (!nameMatches(catalog, tableRef.catalog)) {
                    continue
                }
                val schema = DasUtil.getSchema(table).orEmpty()
                if (!nameMatches(schema, tableRef.schema)) {
                    continue
                }
                val result = LinkedHashMap<String, String>()
                for (column in DasUtil.getColumns(table)) {
                    val comment = column.comment?.trim().orEmpty()
                    if (comment.isNotBlank()) {
                        result[column.name] = comment
                    }
                }
                return result
            }
        }
        return emptyMap()
    }

    private fun nameMatches(actual: String, expected: String): Boolean {
        if (expected.isBlank()) {
            return true
        }
        return actual.equals(expected, ignoreCase = true)
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
            is Date -> value.toLocalDate().toString()
            is Time -> value.toLocalTime().toString()
            is Timestamp -> {
                val utcDateTime = LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC)
                timestampFormatter.format(utcDateTime)
            }
            is UtilDate -> {
                val utcDateTime = LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC)
                val formatted = timestampFormatter.format(utcDateTime)
                if (formatted.endsWith(" 00:00:00")) formatted.substring(0, 10) else formatted
            }
            is TemporalAccessor -> value.toString()
            else -> value
        }
    }

    private data class TableRef(
        val catalog: String,
        val schema: String,
        val table: String
    )
}
