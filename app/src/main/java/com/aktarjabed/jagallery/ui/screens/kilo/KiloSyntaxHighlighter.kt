package com.aktarjabed.jagallery.ui.screens.kilo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Syntax highlighting engine for the Kilo text editor.
 * Supports multiple languages with keyword, string, number, comment, and annotation highlighting.
 */
object KiloSyntaxHighlighter {

    // --- Syntax color palette (dark theme oriented) ---
    val KeywordColor = Color(0xFFC678DD)       // purple
    val TypeColor = Color(0xFFE5C07B)           // gold
    val StringColor = Color(0xFF98C379)         // green
    val NumberColor = Color(0xFFD19A66)          // orange
    val CommentColor = Color(0xFF5C6370)         // grey
    val AnnotationColor = Color(0xFF61AFEF)      // blue
    val FunctionColor = Color(0xFF61AFEF)        // blue
    val OperatorColor = Color(0xFF56B6C2)        // cyan
    val DefaultColor = Color(0xFFABB2BF)         // light grey

    enum class Language(val displayName: String, val extensions: List<String>) {
        KOTLIN("Kotlin", listOf("kt", "kts")),
        JAVA("Java", listOf("java")),
        PYTHON("Python", listOf("py")),
        JAVASCRIPT("JavaScript", listOf("js", "jsx", "ts", "tsx")),
        C("C", listOf("c", "h")),
        CPP("C++", listOf("cpp", "cc", "cxx", "hpp")),
        RUST("Rust", listOf("rs")),
        GO("Go", listOf("go")),
        XML("XML", listOf("xml", "html", "svg")),
        JSON("JSON", listOf("json")),
        MARKDOWN("Markdown", listOf("md", "markdown")),
        SHELL("Shell", listOf("sh", "bash", "zsh")),
        SQL("SQL", listOf("sql")),
        PLAIN_TEXT("Plain Text", listOf("txt", "log", "csv"));

        companion object {
            fun fromExtension(ext: String): Language {
                val lower = ext.lowercase()
                return entries.firstOrNull { lower in it.extensions } ?: PLAIN_TEXT
            }
        }
    }

    private val kotlinKeywords = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
        "companion", "const", "constructor", "continue", "crossinline", "data", "do",
        "else", "enum", "expect", "external", "false", "final", "finally", "for",
        "fun", "get", "if", "import", "in", "infix", "init", "inline", "inner",
        "interface", "internal", "is", "lateinit", "noinline", "null", "object",
        "open", "operator", "out", "override", "package", "private", "protected",
        "public", "reified", "return", "sealed", "set", "super", "suspend",
        "tailrec", "this", "throw", "true", "try", "typealias", "val", "var",
        "vararg", "when", "where", "while"
    )

    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "false", "final", "finally", "float", "for", "goto", "if",
        "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "null", "package", "private", "protected", "public", "return",
        "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "true", "try", "void", "volatile", "while"
    )

    private val pythonKeywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break",
        "class", "continue", "def", "del", "elif", "else", "except", "finally",
        "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
    )

    private val jsKeywords = setOf(
        "abstract", "arguments", "async", "await", "boolean", "break", "byte",
        "case", "catch", "char", "class", "const", "continue", "debugger",
        "default", "delete", "do", "double", "else", "enum", "export", "extends",
        "false", "final", "finally", "float", "for", "from", "function", "goto",
        "if", "implements", "import", "in", "instanceof", "int", "interface",
        "let", "long", "native", "new", "null", "of", "package", "private",
        "protected", "public", "return", "short", "static", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "true", "try",
        "typeof", "undefined", "var", "void", "volatile", "while", "with", "yield"
    )

    private val cKeywords = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do",
        "double", "else", "enum", "extern", "float", "for", "goto", "if", "inline",
        "int", "long", "register", "restrict", "return", "short", "signed",
        "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned",
        "void", "volatile", "while", "_Bool", "_Complex", "_Imaginary"
    )

    private val cppKeywords = cKeywords + setOf(
        "alignas", "alignof", "and", "asm", "bitand", "bitor", "bool", "catch",
        "class", "compl", "const_cast", "constexpr", "decltype", "delete",
        "dynamic_cast", "explicit", "export", "false", "friend", "mutable",
        "namespace", "new", "noexcept", "not", "nullptr", "operator", "or",
        "override", "private", "protected", "public", "reinterpret_cast",
        "static_assert", "static_cast", "template", "this", "thread_local",
        "throw", "true", "try", "typeid", "typename", "using", "virtual", "wchar_t"
    )

    private val rustKeywords = setOf(
        "as", "async", "await", "break", "const", "continue", "crate", "dyn",
        "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
        "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
        "self", "Self", "static", "struct", "super", "trait", "true", "type",
        "unsafe", "use", "where", "while"
    )

    private val goKeywords = setOf(
        "break", "case", "chan", "const", "continue", "default", "defer", "else",
        "fallthrough", "for", "func", "go", "goto", "if", "import", "interface",
        "map", "package", "range", "return", "select", "struct", "switch", "type",
        "var"
    )

    private val sqlKeywords = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE",
        "DROP", "ALTER", "TABLE", "INDEX", "VIEW", "JOIN", "INNER", "OUTER",
        "LEFT", "RIGHT", "ON", "AND", "OR", "NOT", "NULL", "IS", "IN", "BETWEEN",
        "LIKE", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "UNION",
        "ALL", "AS", "DISTINCT", "SET", "VALUES", "PRIMARY", "KEY", "FOREIGN",
        "REFERENCES", "CASCADE", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END",
        "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT", "REVOKE"
    )

    private fun keywordsForLanguage(lang: Language): Set<String> = when (lang) {
        Language.KOTLIN -> kotlinKeywords
        Language.JAVA -> javaKeywords
        Language.PYTHON -> pythonKeywords
        Language.JAVASCRIPT -> jsKeywords
        Language.C -> cKeywords
        Language.CPP -> cppKeywords
        Language.RUST -> rustKeywords
        Language.GO -> goKeywords
        Language.SQL -> sqlKeywords
        else -> emptySet()
    }

    /**
     * Highlights a single line of text for a given language.
     */
    fun highlightLine(line: String, language: Language): AnnotatedString {
        if (language == Language.PLAIN_TEXT || line.isEmpty()) {
            return AnnotatedString(line, SpanStyle(color = DefaultColor))
        }

        if (language == Language.XML) {
            return highlightXml(line)
        }

        if (language == Language.JSON) {
            return highlightJson(line)
        }

        if (language == Language.MARKDOWN) {
            return highlightMarkdown(line)
        }

        val keywords = keywordsForLanguage(language)
        val builder = AnnotatedString.Builder()
        val isCaseSensitiveKeywords = language != Language.SQL

        var i = 0
        while (i < line.length) {
            val ch = line[i]

            // Single-line comment check
            if (ch == '/' && i + 1 < line.length) {
                if (line[i + 1] == '/') {
                    // Rest of line is comment
                    builder.pushStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic))
                    builder.append(line.substring(i))
                    builder.pop()
                    break
                }
            }

            // Python comment
            if (language == Language.PYTHON && ch == '#') {
                builder.pushStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic))
                builder.append(line.substring(i))
                builder.pop()
                break
            }

            // SQL comment --
            if (language == Language.SQL && ch == '-' && i + 1 < line.length && line[i + 1] == '-') {
                builder.pushStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic))
                builder.append(line.substring(i))
                builder.pop()
                break
            }

            // Shell comment
            if (language == Language.SHELL && ch == '#') {
                builder.pushStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic))
                builder.append(line.substring(i))
                builder.pop()
                break
            }

            // String literal
            if (ch == '"' || ch == '\'' || (ch == '`' && language == Language.JAVASCRIPT)) {
                val quote = ch
                val start = i
                i++
                while (i < line.length) {
                    if (line[i] == '\\' && i + 1 < line.length) {
                        i += 2
                        continue
                    }
                    if (line[i] == quote) {
                        i++
                        break
                    }
                    i++
                }
                builder.pushStyle(SpanStyle(color = StringColor))
                builder.append(line.substring(start, i))
                builder.pop()
                continue
            }

            // Annotations (Kotlin/Java: @..., Python: @decorator)
            if (ch == '@' && (language == Language.KOTLIN || language == Language.JAVA || language == Language.PYTHON)) {
                val start = i
                i++
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '.')) {
                    i++
                }
                builder.pushStyle(SpanStyle(color = AnnotationColor))
                builder.append(line.substring(start, i))
                builder.pop()
                continue
            }

            // Numbers
            if (ch.isDigit() || (ch == '.' && i + 1 < line.length && line[i + 1].isDigit())) {
                val start = i
                if (ch == '0' && i + 1 < line.length && (line[i + 1] == 'x' || line[i + 1] == 'X')) {
                    i += 2
                    while (i < line.length && line[i].isLetterOrDigit()) i++
                } else {
                    while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == '_' ||
                                line[i] == 'f' || line[i] == 'F' || line[i] == 'L' || line[i] == 'u' || line[i] == 'U')) {
                        i++
                    }
                }
                builder.pushStyle(SpanStyle(color = NumberColor))
                builder.append(line.substring(start, i))
                builder.pop()
                continue
            }

            // Identifiers / keywords
            if (ch.isLetter() || ch == '_') {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                    i++
                }
                val word = line.substring(start, i)
                val isKeyword = if (isCaseSensitiveKeywords) {
                    word in keywords
                } else {
                    word.uppercase() in keywords
                }

                if (isKeyword) {
                    builder.pushStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold))
                    builder.append(word)
                    builder.pop()
                } else if (word.first().isUpperCase() && word.length > 1) {
                    // Likely a type/class name
                    builder.pushStyle(SpanStyle(color = TypeColor))
                    builder.append(word)
                    builder.pop()
                } else if (i < line.length && line[i] == '(') {
                    // Likely a function call
                    builder.pushStyle(SpanStyle(color = FunctionColor))
                    builder.append(word)
                    builder.pop()
                } else {
                    builder.pushStyle(SpanStyle(color = DefaultColor))
                    builder.append(word)
                    builder.pop()
                }
                continue
            }

            // Operators
            if (ch in "+-*/%=<>!&|^~?:") {
                builder.pushStyle(SpanStyle(color = OperatorColor))
                builder.append(ch.toString())
                builder.pop()
                i++
                continue
            }

            // Default: brackets, whitespace, etc.
            builder.pushStyle(SpanStyle(color = DefaultColor))
            builder.append(ch.toString())
            builder.pop()
            i++
        }

        return builder.toAnnotatedString()
    }

    private fun highlightXml(line: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        while (i < line.length) {
            when {
                // XML comment
                line.startsWith("<!--", i) -> {
                    val end = line.indexOf("-->", i)
                    val commentEnd = if (end != -1) end + 3 else line.length
                    builder.pushStyle(SpanStyle(color = CommentColor, fontStyle = FontStyle.Italic))
                    builder.append(line.substring(i, commentEnd))
                    builder.pop()
                    i = commentEnd
                }
                // Tag
                line[i] == '<' -> {
                    val end = line.indexOf('>', i)
                    val tagEnd = if (end != -1) end + 1 else line.length
                    val tagContent = line.substring(i, tagEnd)
                    highlightXmlTag(builder, tagContent)
                    i = tagEnd
                }
                // String outside tag
                line[i] == '"' || line[i] == '\'' -> {
                    val quote = line[i]
                    val start = i
                    i++
                    while (i < line.length && line[i] != quote) i++
                    if (i < line.length) i++
                    builder.pushStyle(SpanStyle(color = StringColor))
                    builder.append(line.substring(start, i))
                    builder.pop()
                }
                else -> {
                    builder.pushStyle(SpanStyle(color = DefaultColor))
                    builder.append(line[i].toString())
                    builder.pop()
                    i++
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private fun highlightXmlTag(builder: AnnotatedString.Builder, tag: String) {
        // Simple approach: color the brackets, tag name, and attribute strings
        builder.pushStyle(SpanStyle(color = OperatorColor))
        builder.append("<")
        builder.pop()

        val inner = tag.removePrefix("<").removeSuffix(">").let {
            if (it.endsWith("/")) it.dropLast(1) else it
        }
        val trailingSlash = tag.endsWith("/>")
        val closingTag = inner.startsWith("/")
        val stripped = if (closingTag) inner.removePrefix("/") else inner

        val spaceIdx = stripped.indexOfFirst { it.isWhitespace() }
        val tagName = if (spaceIdx != -1) stripped.substring(0, spaceIdx) else stripped
        val attrs = if (spaceIdx != -1) stripped.substring(spaceIdx) else ""

        if (closingTag) {
            builder.pushStyle(SpanStyle(color = OperatorColor))
            builder.append("/")
            builder.pop()
        }

        builder.pushStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold))
        builder.append(tagName)
        builder.pop()

        if (attrs.isNotEmpty()) {
            highlightXmlAttributes(builder, attrs)
        }

        if (trailingSlash) {
            builder.pushStyle(SpanStyle(color = OperatorColor))
            builder.append("/")
            builder.pop()
        }

        builder.pushStyle(SpanStyle(color = OperatorColor))
        builder.append(">")
        builder.pop()
    }

    private fun highlightXmlAttributes(builder: AnnotatedString.Builder, attrs: String) {
        var i = 0
        while (i < attrs.length) {
            when {
                attrs[i].isWhitespace() -> {
                    builder.append(attrs[i].toString())
                    i++
                }
                attrs[i] == '"' || attrs[i] == '\'' -> {
                    val quote = attrs[i]
                    val start = i
                    i++
                    while (i < attrs.length && attrs[i] != quote) i++
                    if (i < attrs.length) i++
                    builder.pushStyle(SpanStyle(color = StringColor))
                    builder.append(attrs.substring(start, i))
                    builder.pop()
                }
                attrs[i] == '=' -> {
                    builder.pushStyle(SpanStyle(color = OperatorColor))
                    builder.append("=")
                    builder.pop()
                    i++
                }
                else -> {
                    val start = i
                    while (i < attrs.length && !attrs[i].isWhitespace() && attrs[i] != '=' && attrs[i] != '"' && attrs[i] != '\'') i++
                    builder.pushStyle(SpanStyle(color = AnnotationColor))
                    builder.append(attrs.substring(start, i))
                    builder.pop()
                }
            }
        }
    }

    private fun highlightJson(line: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    val start = i
                    i++
                    while (i < line.length) {
                        if (line[i] == '\\' && i + 1 < line.length) { i += 2; continue }
                        if (line[i] == '"') { i++; break }
                        i++
                    }
                    val str = line.substring(start, i)
                    // Check if this is a key (followed by :)
                    val afterStr = line.substring(i).trimStart()
                    if (afterStr.startsWith(":")) {
                        builder.pushStyle(SpanStyle(color = AnnotationColor))
                    } else {
                        builder.pushStyle(SpanStyle(color = StringColor))
                    }
                    builder.append(str)
                    builder.pop()
                }
                ch.isDigit() || ch == '-' -> {
                    val start = i
                    if (ch == '-') i++
                    while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] == 'e' || line[i] == 'E' || line[i] == '+' || line[i] == '-')) i++
                    builder.pushStyle(SpanStyle(color = NumberColor))
                    builder.append(line.substring(start, i))
                    builder.pop()
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < line.length && line[i].isLetter()) i++
                    val word = line.substring(start, i)
                    if (word in setOf("true", "false", "null")) {
                        builder.pushStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold))
                    } else {
                        builder.pushStyle(SpanStyle(color = DefaultColor))
                    }
                    builder.append(word)
                    builder.pop()
                }
                ch in "{}[]:" -> {
                    builder.pushStyle(SpanStyle(color = OperatorColor))
                    builder.append(ch.toString())
                    builder.pop()
                    i++
                }
                else -> {
                    builder.pushStyle(SpanStyle(color = DefaultColor))
                    builder.append(ch.toString())
                    builder.pop()
                    i++
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private fun highlightMarkdown(line: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ") ||
                    trimmed.startsWith("#### ") || trimmed.startsWith("##### ") || trimmed.startsWith("###### ") -> {
                builder.pushStyle(SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold))
                builder.append(line)
                builder.pop()
            }
            trimmed.startsWith("```") -> {
                builder.pushStyle(SpanStyle(color = AnnotationColor))
                builder.append(line)
                builder.pop()
            }
            trimmed.startsWith("> ") -> {
                builder.pushStyle(SpanStyle(color = StringColor, fontStyle = FontStyle.Italic))
                builder.append(line)
                builder.pop()
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                builder.pushStyle(SpanStyle(color = OperatorColor))
                val bulletEnd = line.indexOf(' ') + 1
                builder.append(line.substring(0, bulletEnd))
                builder.pop()
                builder.pushStyle(SpanStyle(color = DefaultColor))
                builder.append(line.substring(bulletEnd))
                builder.pop()
            }
            else -> {
                builder.pushStyle(SpanStyle(color = DefaultColor))
                builder.append(line)
                builder.pop()
            }
        }
        return builder.toAnnotatedString()
    }
}
