package com.example.personalmusicplayer.ui.playlists

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Перетаскивание элементов LazyColumn пальцем. Элементы должны иметь key и занимать
 * всё содержимое списка (без шапок внутри LazyColumn), чтобы индексы совпадали с порядком данных.
 */
class ReorderState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnd: () -> Unit,
) {
    var draggedKey by mutableStateOf<Any?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set

    /** Ждём, пока список перестроится после перестановки, прежде чем делать следующую. */
    private var pendingIndex: Int? = null

    fun start(key: Any) {
        draggedKey = key
        dragOffset = 0f
        pendingIndex = null
    }

    fun dragBy(delta: Float) {
        val key = draggedKey ?: return
        dragOffset += delta

        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == key } ?: return
        pendingIndex?.let { expected ->
            if (current.index != expected) return
            pendingIndex = null
        }

        val center = current.offset + dragOffset + current.size / 2f
        val target = visible.firstOrNull {
            it.key != key && center >= it.offset && center <= it.offset + it.size
        } ?: return

        onMove(current.index, target.index)
        // Элемент перескочил на место соседа; компенсируем сдвиг, чтобы он остался под пальцем.
        dragOffset += (current.offset - target.offset)
        pendingIndex = target.index
    }

    fun end() {
        if (draggedKey == null) return
        draggedKey = null
        dragOffset = 0f
        pendingIndex = null
        onDragEnd()
    }
}

/** То же для сетки (LazyVerticalGrid): элемент можно двигать в обе стороны. */
class GridReorderState(
    private val gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnd: () -> Unit,
) {
    var draggedKey by mutableStateOf<Any?>(null)
        private set
    var dragOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
        private set

    private var pendingIndex: Int? = null

    fun start(key: Any) {
        draggedKey = key
        dragOffset = androidx.compose.ui.geometry.Offset.Zero
        pendingIndex = null
    }

    fun dragBy(delta: androidx.compose.ui.geometry.Offset) {
        val key = draggedKey ?: return
        dragOffset += delta

        val visible = gridState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == key } ?: return
        pendingIndex?.let { expected ->
            if (current.index != expected) return
            pendingIndex = null
        }

        val centerX = current.offset.x + dragOffset.x + current.size.width / 2f
        val centerY = current.offset.y + dragOffset.y + current.size.height / 2f
        val target = visible.firstOrNull {
            it.key != key &&
                centerX >= it.offset.x && centerX <= it.offset.x + it.size.width &&
                centerY >= it.offset.y && centerY <= it.offset.y + it.size.height
        } ?: return

        onMove(current.index, target.index)
        dragOffset += androidx.compose.ui.geometry.Offset(
            (current.offset.x - target.offset.x).toFloat(),
            (current.offset.y - target.offset.y).toFloat(),
        )
        pendingIndex = target.index
    }

    fun end() {
        if (draggedKey == null) return
        draggedKey = null
        dragOffset = androidx.compose.ui.geometry.Offset.Zero
        pendingIndex = null
        onDragEnd()
    }
}
