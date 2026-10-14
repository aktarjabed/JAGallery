package com.aktarjabed.jagallery.ui.screens.kilo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class KiloCursor(
    val line: Int = 0,
    val column: Int = 0
)

data class KiloSearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val matches: List<Pair<Int, Int>> = emptyList(), // (line, column) pairs
    val currentMatchIndex: Int = -1
)

sealed class KiloMessage {
    data class Info(val text: String) : KiloMessage()
    data class Error(val text: String) : KiloMessage()
    data class Warning(val text: String) : KiloMessage()
}

@HiltViewModel
class KiloViewModel @Inject constructor() : ViewModel() {

    // --- Editor content ---
    private val _lines = MutableStateFlow<List<String>>(listOf(""))
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    // --- Cursor ---
    private val _cursor = MutableStateFlow(KiloCursor())
    val cursor: StateFlow<KiloCursor> = _cursor.asStateFlow()

    // --- Selection ---
    private val _selectionStart = MutableStateFlow<KiloCursor?>(null)
    val selectionStart: StateFlow<KiloCursor?> = _selectionStart.asStateFlow()

    private val _selectionEnd = MutableStateFlow<KiloCursor?>(null)
    val selectionEnd: StateFlow<KiloCursor?> = _selectionEnd.asStateFlow()

    // --- File info ---
    private val _fileName = MutableStateFlow("untitled.txt")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _fileUri = MutableStateFlow<Uri?>(null)
    val fileUri: StateFlow<Uri?> = _fileUri.asStateFlow()

    private val _language = MutableStateFlow(KiloSyntaxHighlighter.Language.PLAIN_TEXT)
    val language: StateFlow<KiloSyntaxHighlighter.Language> = _language.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    // --- Status / messages ---
    private val _statusMessage = MutableStateFlow<KiloMessage?>(null)
    val statusMessage: StateFlow<KiloMessage?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Search ---
    private val _searchState = MutableStateFlow(KiloSearchState())
    val searchState: StateFlow<KiloSearchState> = _searchState.asStateFlow()

    // --- Undo/Redo ---
    private data class EditorSnapshot(
        val lines: List<String>,
        val cursor: KiloCursor
    )

    private val undoStack = ArrayDeque<EditorSnapshot>(MAX_UNDO_HISTORY)
    private val redoStack = ArrayDeque<EditorSnapshot>(MAX_UNDO_HISTORY)

    // --- Scroll offset (for viewport tracking) ---
    private val _scrollOffset = MutableStateFlow(0)
    val scrollOffset: StateFlow<Int> = _scrollOffset.asStateFlow()

    // --- Tab size ---
    private val _tabSize = MutableStateFlow(4)
    val tabSize: StateFlow<Int> = _tabSize.asStateFlow()

    // --- Word wrap ---
    private val _wordWrap = MutableStateFlow(false)
    val wordWrap: StateFlow<Boolean> = _wordWrap.asStateFlow()

    // --- Show line numbers ---
    private val _showLineNumbers = MutableStateFlow(true)
    val showLineNumbers: StateFlow<Boolean> = _showLineNumbers.asStateFlow()

    companion object {
        const val MAX_UNDO_HISTORY = 200
    }

    // ====================== FILE OPERATIONS ======================

    fun openFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val name = getFileNameFromUri(context, uri) ?: "unknown"
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).readText()
                    } ?: ""
                }
                val fileLines = if (content.isEmpty()) listOf("") else content.lines()

                _fileUri.value = uri
                _fileName.value = name
                _lines.value = fileLines
                _cursor.value = KiloCursor(0, 0)
                _isDirty.value = false
                _selectionStart.value = null
                _selectionEnd.value = null
                undoStack.clear()
                redoStack.clear()

                // Detect language from extension
                val ext = name.substringAfterLast('.', "")
                _language.value = KiloSyntaxHighlighter.Language.fromExtension(ext)

                _statusMessage.value = KiloMessage.Info("Opened \"$name\" — ${fileLines.size} lines")
            } catch (e: Exception) {
                _statusMessage.value = KiloMessage.Error("Failed to open file: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFile(context: Context) {
        val uri = _fileUri.value
        if (uri == null) {
            _statusMessage.value = KiloMessage.Warning("No file open. Use \"Save As\" to create a new file.")
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val content = _lines.value.joinToString("\n")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    } ?: throw Exception("Could not open output stream")
                }
                _isDirty.value = false
                _statusMessage.value = KiloMessage.Info("Saved \"${_fileName.value}\"")
            } catch (e: Exception) {
                _statusMessage.value = KiloMessage.Error("Failed to save: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFileAs(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val content = _lines.value.joinToString("\n")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    } ?: throw Exception("Could not open output stream")
                }
                val name = getFileNameFromUri(context, uri) ?: "unknown"
                _fileUri.value = uri
                _fileName.value = name
                _isDirty.value = false

                val ext = name.substringAfterLast('.', "")
                _language.value = KiloSyntaxHighlighter.Language.fromExtension(ext)

                _statusMessage.value = KiloMessage.Info("Saved as \"$name\"")
            } catch (e: Exception) {
                _statusMessage.value = KiloMessage.Error("Failed to save: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun newFile() {
        _lines.value = listOf("")
        _cursor.value = KiloCursor(0, 0)
        _fileName.value = "untitled.txt"
        _fileUri.value = null
        _language.value = KiloSyntaxHighlighter.Language.PLAIN_TEXT
        _isDirty.value = false
        _selectionStart.value = null
        _selectionEnd.value = null
        undoStack.clear()
        redoStack.clear()
        _statusMessage.value = KiloMessage.Info("New file created")
    }

    // ====================== EDITING OPERATIONS ======================

    private fun pushUndo() {
        if (undoStack.size >= MAX_UNDO_HISTORY) {
            undoStack.removeFirst()
        }
        undoStack.addLast(EditorSnapshot(_lines.value.toList(), _cursor.value))
        redoStack.clear()
    }

    fun insertText(text: String) {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value

        // Handle selected text replacement
        deleteSelection(currentLines)

        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val col = cur.column.coerceIn(0, currentLines[line].length)

        val before = currentLines[line].substring(0, col)
        val after = currentLines[line].substring(col)

        if (text.contains('\n')) {
            val insertLines = text.split('\n')
            currentLines[line] = before + insertLines.first()
            for (i in 1 until insertLines.size) {
                currentLines.add(line + i, insertLines[i])
            }
            val lastInsertedLine = line + insertLines.size - 1
            currentLines[lastInsertedLine] = currentLines[lastInsertedLine] + after
            _cursor.value = KiloCursor(lastInsertedLine, currentLines[lastInsertedLine].length - after.length)
        } else {
            currentLines[line] = before + text + after
            _cursor.value = KiloCursor(line, col + text.length)
        }

        _lines.value = currentLines
        _isDirty.value = true
        clearSelection()
    }

    fun insertNewLine() {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        deleteSelection(currentLines)

        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val col = cur.column.coerceIn(0, currentLines[line].length)

        val before = currentLines[line].substring(0, col)
        val after = currentLines[line].substring(col)

        // Auto-indent: copy leading whitespace from current line
        val indent = before.takeWhile { it == ' ' || it == '\t' }

        currentLines[line] = before
        currentLines.add(line + 1, indent + after)
        _cursor.value = KiloCursor(line + 1, indent.length)
        _lines.value = currentLines
        _isDirty.value = true
        clearSelection()
    }

    fun insertTab() {
        val spaces = " ".repeat(_tabSize.value)
        insertText(spaces)
    }

    fun deleteCharBackward() {
        if (hasSelection()) {
            pushUndo()
            val currentLines = _lines.value.toMutableList()
            deleteSelection(currentLines)
            _lines.value = currentLines
            _isDirty.value = true
            clearSelection()
            return
        }

        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val col = cur.column.coerceIn(0, currentLines[line].length)

        if (col > 0) {
            currentLines[line] = currentLines[line].removeRange(col - 1, col)
            _cursor.value = KiloCursor(line, col - 1)
        } else if (line > 0) {
            val prevLineLen = currentLines[line - 1].length
            currentLines[line - 1] = currentLines[line - 1] + currentLines[line]
            currentLines.removeAt(line)
            _cursor.value = KiloCursor(line - 1, prevLineLen)
        }
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun deleteCharForward() {
        if (hasSelection()) {
            pushUndo()
            val currentLines = _lines.value.toMutableList()
            deleteSelection(currentLines)
            _lines.value = currentLines
            _isDirty.value = true
            clearSelection()
            return
        }

        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val col = cur.column.coerceIn(0, currentLines[line].length)

        if (col < currentLines[line].length) {
            currentLines[line] = currentLines[line].removeRange(col, col + 1)
        } else if (line < currentLines.lastIndex) {
            currentLines[line] = currentLines[line] + currentLines[line + 1]
            currentLines.removeAt(line + 1)
        }
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun deleteWordBackward() {
        if (hasSelection()) {
            deleteCharBackward()
            return
        }
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val col = cur.column.coerceIn(0, currentLines[line].length)

        if (col == 0 && line > 0) {
            // Join with previous line
            val prevLineLen = currentLines[line - 1].length
            currentLines[line - 1] = currentLines[line - 1] + currentLines[line]
            currentLines.removeAt(line)
            _cursor.value = KiloCursor(line - 1, prevLineLen)
        } else if (col > 0) {
            val lineText = currentLines[line]
            var newCol = col - 1
            // Skip whitespace
            while (newCol > 0 && lineText[newCol - 1].isWhitespace()) newCol--
            // Skip word characters
            while (newCol > 0 && !lineText[newCol - 1].isWhitespace()) newCol--
            currentLines[line] = lineText.removeRange(newCol, col)
            _cursor.value = KiloCursor(line, newCol)
        }
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun deleteLine() {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)

        if (currentLines.size > 1) {
            currentLines.removeAt(line)
            val newLine = line.coerceAtMost(currentLines.lastIndex)
            _cursor.value = KiloCursor(newLine, _cursor.value.column.coerceAtMost(currentLines[newLine].length))
        } else {
            currentLines[0] = ""
            _cursor.value = KiloCursor(0, 0)
        }
        _lines.value = currentLines
        _isDirty.value = true
        clearSelection()
    }

    fun duplicateLine() {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        currentLines.add(line + 1, currentLines[line])
        _cursor.value = KiloCursor(line + 1, cur.column)
        _lines.value = currentLines
        _isDirty.value = true
    }

    // ====================== CURSOR MOVEMENT ======================

    fun moveCursorTo(line: Int, column: Int) {
        val clampedLine = line.coerceIn(0, (_lines.value.size - 1).coerceAtLeast(0))
        val clampedCol = column.coerceIn(0, _lines.value.getOrElse(clampedLine) { "" }.length)
        _cursor.value = KiloCursor(clampedLine, clampedCol)
        clearSelection()
    }

    fun moveCursorUp() {
        val cur = _cursor.value
        if (cur.line > 0) {
            val newLine = cur.line - 1
            _cursor.value = KiloCursor(newLine, cur.column.coerceAtMost(_lines.value[newLine].length))
        }
        clearSelection()
    }

    fun moveCursorDown() {
        val cur = _cursor.value
        if (cur.line < _lines.value.lastIndex) {
            val newLine = cur.line + 1
            _cursor.value = KiloCursor(newLine, cur.column.coerceAtMost(_lines.value[newLine].length))
        }
        clearSelection()
    }

    fun moveCursorLeft() {
        val cur = _cursor.value
        if (cur.column > 0) {
            _cursor.value = KiloCursor(cur.line, cur.column - 1)
        } else if (cur.line > 0) {
            _cursor.value = KiloCursor(cur.line - 1, _lines.value[cur.line - 1].length)
        }
        clearSelection()
    }

    fun moveCursorRight() {
        val cur = _cursor.value
        val lineLen = _lines.value.getOrElse(cur.line) { "" }.length
        if (cur.column < lineLen) {
            _cursor.value = KiloCursor(cur.line, cur.column + 1)
        } else if (cur.line < _lines.value.lastIndex) {
            _cursor.value = KiloCursor(cur.line + 1, 0)
        }
        clearSelection()
    }

    fun moveCursorToLineStart() {
        val cur = _cursor.value
        val line = _lines.value.getOrElse(cur.line) { "" }
        val firstNonWhitespace = line.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0
        // Toggle between first non-whitespace and absolute start
        val newCol = if (cur.column == firstNonWhitespace) 0 else firstNonWhitespace
        _cursor.value = KiloCursor(cur.line, newCol)
        clearSelection()
    }

    fun moveCursorToLineEnd() {
        val cur = _cursor.value
        _cursor.value = KiloCursor(cur.line, _lines.value.getOrElse(cur.line) { "" }.length)
        clearSelection()
    }

    fun moveCursorToDocStart() {
        _cursor.value = KiloCursor(0, 0)
        clearSelection()
    }

    fun moveCursorToDocEnd() {
        val lastLine = (_lines.value.size - 1).coerceAtLeast(0)
        _cursor.value = KiloCursor(lastLine, _lines.value.getOrElse(lastLine) { "" }.length)
        clearSelection()
    }

    fun moveCursorWordLeft() {
        val cur = _cursor.value
        val line = _lines.value.getOrElse(cur.line) { "" }
        if (cur.column == 0 && cur.line > 0) {
            _cursor.value = KiloCursor(cur.line - 1, _lines.value[cur.line - 1].length)
        } else {
            var col = cur.column - 1
            while (col > 0 && line[col - 1].isWhitespace()) col--
            while (col > 0 && !line[col - 1].isWhitespace()) col--
            _cursor.value = KiloCursor(cur.line, col.coerceAtLeast(0))
        }
        clearSelection()
    }

    fun moveCursorWordRight() {
        val cur = _cursor.value
        val line = _lines.value.getOrElse(cur.line) { "" }
        if (cur.column >= line.length && cur.line < _lines.value.lastIndex) {
            _cursor.value = KiloCursor(cur.line + 1, 0)
        } else {
            var col = cur.column
            while (col < line.length && !line[col].isWhitespace()) col++
            while (col < line.length && line[col].isWhitespace()) col++
            _cursor.value = KiloCursor(cur.line, col.coerceAtMost(line.length))
        }
        clearSelection()
    }

    // ====================== SELECTION ======================

    private fun hasSelection(): Boolean {
        val start = _selectionStart.value ?: return false
        val end = _selectionEnd.value ?: return false
        return start != end
    }

    fun setSelection(startLine: Int, startCol: Int, endLine: Int, endCol: Int) {
        _selectionStart.value = KiloCursor(startLine, startCol)
        _selectionEnd.value = KiloCursor(endLine, endCol)
    }

    fun selectAll() {
        val lastLine = (_lines.value.size - 1).coerceAtLeast(0)
        _selectionStart.value = KiloCursor(0, 0)
        _selectionEnd.value = KiloCursor(lastLine, _lines.value.getOrElse(lastLine) { "" }.length)
        _cursor.value = KiloCursor(lastLine, _lines.value.getOrElse(lastLine) { "" }.length)
    }

    fun selectLine() {
        val cur = _cursor.value
        val lineLen = _lines.value.getOrElse(cur.line) { "" }.length
        _selectionStart.value = KiloCursor(cur.line, 0)
        _selectionEnd.value = KiloCursor(cur.line, lineLen)
    }

    fun selectWord() {
        val cur = _cursor.value
        val line = _lines.value.getOrElse(cur.line) { "" }
        if (line.isEmpty()) return

        val col = cur.column.coerceAtMost(line.lastIndex)
        var start = col
        var end = col

        while (start > 0 && line[start - 1].isLetterOrDigit()) start--
        while (end < line.length && line[end].isLetterOrDigit()) end++

        _selectionStart.value = KiloCursor(cur.line, start)
        _selectionEnd.value = KiloCursor(cur.line, end)
    }

    fun getSelectedText(): String {
        val start = _selectionStart.value ?: return ""
        val end = _selectionEnd.value ?: return ""

        val (s, e) = if (start.line < end.line || (start.line == end.line && start.column <= end.column)) {
            start to end
        } else {
            end to start
        }

        val currentLines = _lines.value
        return if (s.line == e.line) {
            currentLines.getOrElse(s.line) { "" }.substring(
                s.column.coerceIn(0, currentLines[s.line].length),
                e.column.coerceIn(0, currentLines[e.line].length)
            )
        } else {
            val builder = StringBuilder()
            builder.append(currentLines.getOrElse(s.line) { "" }.substring(s.column.coerceIn(0, currentLines[s.line].length)))
            for (i in s.line + 1 until e.line) {
                builder.append('\n')
                builder.append(currentLines.getOrElse(i) { "" })
            }
            builder.append('\n')
            builder.append(currentLines.getOrElse(e.line) { "" }.substring(0, e.column.coerceIn(0, currentLines[e.line].length)))
            builder.toString()
        }
    }

    private fun clearSelection() {
        _selectionStart.value = null
        _selectionEnd.value = null
    }

    private fun deleteSelection(mutableLines: MutableList<String>) {
        val start = _selectionStart.value ?: return
        val end = _selectionEnd.value ?: return

        val (s, e) = if (start.line < end.line || (start.line == end.line && start.column <= end.column)) {
            start to end
        } else {
            end to start
        }

        if (s.line == e.line) {
            val sCol = s.column.coerceIn(0, mutableLines[s.line].length)
            val eCol = e.column.coerceIn(0, mutableLines[e.line].length)
            mutableLines[s.line] = mutableLines[s.line].removeRange(sCol, eCol)
            _cursor.value = KiloCursor(s.line, sCol)
        } else {
            val sCol = s.column.coerceIn(0, mutableLines[s.line].length)
            val eCol = e.column.coerceIn(0, mutableLines[e.line].length)
            val before = mutableLines[s.line].substring(0, sCol)
            val after = mutableLines[e.line].substring(eCol)
            // Remove intermediate lines
            for (i in e.line downTo s.line + 1) {
                mutableLines.removeAt(i)
            }
            mutableLines[s.line] = before + after
            _cursor.value = KiloCursor(s.line, sCol)
        }
        clearSelection()
    }

    // ====================== UNDO / REDO ======================

    fun undo() {
        if (undoStack.isEmpty()) {
            _statusMessage.value = KiloMessage.Warning("Nothing to undo")
            return
        }
        redoStack.addLast(EditorSnapshot(_lines.value.toList(), _cursor.value))
        val snapshot = undoStack.removeLast()
        _lines.value = snapshot.lines
        _cursor.value = snapshot.cursor
        _isDirty.value = true
        clearSelection()
    }

    fun redo() {
        if (redoStack.isEmpty()) {
            _statusMessage.value = KiloMessage.Warning("Nothing to redo")
            return
        }
        undoStack.addLast(EditorSnapshot(_lines.value.toList(), _cursor.value))
        val snapshot = redoStack.removeLast()
        _lines.value = snapshot.lines
        _cursor.value = snapshot.cursor
        _isDirty.value = true
        clearSelection()
    }

    // ====================== SEARCH ======================

    fun openSearch() {
        _searchState.value = _searchState.value.copy(isActive = true)
    }

    fun closeSearch() {
        _searchState.value = KiloSearchState()
    }

    fun updateSearchQuery(query: String) {
        if (query.isEmpty()) {
            _searchState.value = _searchState.value.copy(query = "", matches = emptyList(), currentMatchIndex = -1)
            return
        }

        val matches = mutableListOf<Pair<Int, Int>>()
        val lowerQuery = query.lowercase()
        _lines.value.forEachIndexed { lineIdx, lineStr ->
            var searchStart = 0
            val lowerLine = lineStr.lowercase()
            while (true) {
                val idx = lowerLine.indexOf(lowerQuery, searchStart)
                if (idx == -1) break
                matches.add(lineIdx to idx)
                searchStart = idx + 1
            }
        }
        _searchState.value = _searchState.value.copy(
            query = query,
            matches = matches,
            currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
        )

        if (matches.isNotEmpty()) {
            val firstMatch = matches[0]
            _cursor.value = KiloCursor(firstMatch.first, firstMatch.second)
        }
    }

    fun searchNext() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val nextIdx = (state.currentMatchIndex + 1) % state.matches.size
        _searchState.value = state.copy(currentMatchIndex = nextIdx)
        val match = state.matches[nextIdx]
        _cursor.value = KiloCursor(match.first, match.second)
    }

    fun searchPrev() {
        val state = _searchState.value
        if (state.matches.isEmpty()) return
        val prevIdx = if (state.currentMatchIndex - 1 < 0) state.matches.lastIndex else state.currentMatchIndex - 1
        _searchState.value = state.copy(currentMatchIndex = prevIdx)
        val match = state.matches[prevIdx]
        _cursor.value = KiloCursor(match.first, match.second)
    }

    fun replaceCurrentMatch(replacement: String) {
        val state = _searchState.value
        if (state.matches.isEmpty() || state.currentMatchIndex < 0) return
        val match = state.matches[state.currentMatchIndex]

        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val line = match.first
        val col = match.second
        val lineText = currentLines[line]
        currentLines[line] = lineText.substring(0, col) + replacement + lineText.substring(col + state.query.length)
        _lines.value = currentLines
        _isDirty.value = true

        // Re-run search to update matches
        updateSearchQuery(state.query)
    }

    fun replaceAll(replacement: String) {
        val state = _searchState.value
        if (state.query.isEmpty()) return

        pushUndo()
        val currentLines = _lines.value.toMutableList()
        for (i in currentLines.indices) {
            currentLines[i] = currentLines[i].replace(state.query, replacement, ignoreCase = true)
        }
        _lines.value = currentLines
        _isDirty.value = true
        updateSearchQuery(state.query)
        _statusMessage.value = KiloMessage.Info("Replaced all occurrences")
    }

    // ====================== SETTINGS ======================

    fun toggleWordWrap() {
        _wordWrap.value = !_wordWrap.value
    }

    fun toggleLineNumbers() {
        _showLineNumbers.value = !_showLineNumbers.value
    }

    fun setTabSize(size: Int) {
        _tabSize.value = size.coerceIn(1, 8)
    }

    fun setLanguage(language: KiloSyntaxHighlighter.Language) {
        _language.value = language
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun setScrollOffset(offset: Int) {
        _scrollOffset.value = offset
    }

    // ====================== LINE OPERATIONS ======================

    fun moveLineUp() {
        val cur = _cursor.value
        if (cur.line <= 0) return
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val temp = currentLines[cur.line]
        currentLines[cur.line] = currentLines[cur.line - 1]
        currentLines[cur.line - 1] = temp
        _cursor.value = KiloCursor(cur.line - 1, cur.column.coerceAtMost(currentLines[cur.line - 1].length))
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun moveLineDown() {
        val cur = _cursor.value
        if (cur.line >= _lines.value.lastIndex) return
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val temp = currentLines[cur.line]
        currentLines[cur.line] = currentLines[cur.line + 1]
        currentLines[cur.line + 1] = temp
        _cursor.value = KiloCursor(cur.line + 1, cur.column.coerceAtMost(currentLines[cur.line + 1].length))
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun indentLine() {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val spaces = " ".repeat(_tabSize.value)
        currentLines[line] = spaces + currentLines[line]
        _cursor.value = KiloCursor(line, cur.column + _tabSize.value)
        _lines.value = currentLines
        _isDirty.value = true
    }

    fun unindentLine() {
        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val lineText = currentLines[line]
        val spacesToRemove = lineText.takeWhile { it == ' ' }.length.coerceAtMost(_tabSize.value)
        if (spacesToRemove > 0) {
            currentLines[line] = lineText.substring(spacesToRemove)
            _cursor.value = KiloCursor(line, (cur.column - spacesToRemove).coerceAtLeast(0))
            _lines.value = currentLines
            _isDirty.value = true
        }
    }

    fun toggleComment() {
        val commentPrefix = when (_language.value) {
            KiloSyntaxHighlighter.Language.KOTLIN, KiloSyntaxHighlighter.Language.JAVA,
            KiloSyntaxHighlighter.Language.JAVASCRIPT, KiloSyntaxHighlighter.Language.C,
            KiloSyntaxHighlighter.Language.CPP, KiloSyntaxHighlighter.Language.RUST,
            KiloSyntaxHighlighter.Language.GO -> "//"
            KiloSyntaxHighlighter.Language.PYTHON, KiloSyntaxHighlighter.Language.SHELL -> "#"
            KiloSyntaxHighlighter.Language.SQL -> "--"
            else -> return
        }

        pushUndo()
        val currentLines = _lines.value.toMutableList()
        val cur = _cursor.value
        val line = cur.line.coerceIn(0, currentLines.lastIndex)
        val lineText = currentLines[line]
        val trimmed = lineText.trimStart()

        if (trimmed.startsWith(commentPrefix)) {
            // Uncomment
            val idx = lineText.indexOf(commentPrefix)
            val afterComment = lineText.substring(idx + commentPrefix.length)
            val uncommented = lineText.substring(0, idx) + (if (afterComment.startsWith(" ")) afterComment.substring(1) else afterComment)
            currentLines[line] = uncommented
        } else {
            // Comment
            val indent = lineText.takeWhile { it.isWhitespace() }
            currentLines[line] = indent + commentPrefix + " " + lineText.trimStart()
        }
        _lines.value = currentLines
        _isDirty.value = true
    }

    // ====================== UTILS ======================

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    // ====================== GO TO LINE ======================

    fun goToLine(lineNumber: Int) {
        val targetLine = (lineNumber - 1).coerceIn(0, (_lines.value.size - 1).coerceAtLeast(0))
        _cursor.value = KiloCursor(targetLine, 0)
        clearSelection()
        _statusMessage.value = KiloMessage.Info("Jumped to line ${targetLine + 1}")
    }
}
