package com.example.datagripjson

import com.intellij.database.datagrid.DataGrid
import com.intellij.database.datagrid.DataGridListener
import com.intellij.database.datagrid.GridRequestSource
import com.intellij.database.datagrid.GridColumn
import com.intellij.database.datagrid.ModelIndex
import com.intellij.database.extractors.DisplayType
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

@Service(Service.Level.PROJECT)
class RowJsonViewState(project: Project) {
    var panel: JsonViewerPanel? = null
    var lastActiveGrid: DataGrid? = null
    private var trackedFocus: Component? = null
    private var observedGrid: DataGrid? = null
    private var gridListenerDisposable: Disposable? = null
    private val focusListener = PropertyChangeListener { event: PropertyChangeEvent ->
        if (event.propertyName != "permanentFocusOwner") {
            return@PropertyChangeListener
        }
        trackedFocus = event.newValue as? Component
        CurrentRowExtractor.findDataGrid(trackedFocus)?.let { grid ->
            activateGrid(grid)
        }
    }

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(DataGrid.ACTIVE_GRID_CHANGED_TOPIC, object : DataGrid.ActiveGridListener {
            override fun changed(dataGrid: DataGrid) {
                activateGrid(dataGrid)
            }

            override fun closed() {
                lastActiveGrid = null
                detachGridListener()
            }

            override fun onFilterApplied(dataGrid: DataGrid) {}
            override fun onSortingApplied(dataGrid: DataGrid) {}
            override fun onColumnSortingToggled(dataGrid: DataGrid) {}
            override fun onValueEditorOpened(dataGrid: DataGrid) {}
            override fun onAggregateViewOpened(dataGrid: DataGrid) {}
            override fun onRecordViewOpened(dataGrid: DataGrid) {}
            override fun onExtractToClipboardAction(dataGrid: DataGrid) {}
            override fun onExtractToFileAction(dataGrid: DataGrid) {}
        })

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addPropertyChangeListener("permanentFocusOwner", focusListener)
    }

    fun trackedFocusComponent(): Component? = trackedFocus

    fun updateActiveGrid(dataGrid: DataGrid) {
        activateGrid(dataGrid)
    }

    fun syncPanelWithCurrentSelection() {
        pushCurrentSelection()
    }

    private fun activateGrid(dataGrid: DataGrid) {
        lastActiveGrid = dataGrid
        attachGridListener(dataGrid)
        pushCurrentSelection()
    }

    private fun pushCurrentSelection() {
        val json = CurrentRowExtractor.extractFromGrid(lastActiveGrid)?.let(JsonFormatter::format) ?: return
        panel?.setJson(json)
    }

    private fun attachGridListener(dataGrid: DataGrid) {
        if (observedGrid === dataGrid) {
            return
        }
        detachGridListener()
        observedGrid = dataGrid
        val disposable = Disposer.newDisposable("RowJsonViewerGridListener")
        dataGrid.addDataGridListener(object : DataGridListener {
            override fun onSelectionChanged(grid: DataGrid) {
                if (grid === lastActiveGrid) {
                    pushCurrentSelection()
                }
            }

            override fun onSelectionChanged(grid: DataGrid, adjusting: Boolean) {
                if (!adjusting) {
                    onSelectionChanged(grid)
                }
            }

            override fun onContentChanged(grid: DataGrid, requestPlace: GridRequestSource.RequestPlace?) {}
            override fun onCellLanguageChanged(columnIdx: ModelIndex<GridColumn>, language: Language) {}
            override fun onValueEdited(grid: DataGrid, value: Any?) {}
            override fun onCellDisplayTypeChanged(columnIdx: ModelIndex<GridColumn>, displayType: DisplayType) {}
        }, disposable)
        gridListenerDisposable = disposable
    }

    private fun detachGridListener() {
        gridListenerDisposable?.let { Disposer.dispose(it) }
        gridListenerDisposable = null
        observedGrid = null
    }

    companion object {
        fun getInstance(project: Project): RowJsonViewState = project.getService(RowJsonViewState::class.java)
    }
}
