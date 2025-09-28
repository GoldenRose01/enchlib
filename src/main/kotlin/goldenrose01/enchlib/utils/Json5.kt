package goldenrose01.enchlib.utils

/**
 * Semplice sanitizer per file JSON5:
 * - Rimuove commenti // e /* */
 * - Rimuove trailing commas prima di } o ]
 */
object Json5 {
    private val lineCommentRegex = Regex("""(?m)//.*$""")
    private val blockCommentRegex = Regex("""(?s)/\*.*?\*/""")
    private val trailingCommaRegex = Regex(""",\s*([}\]])""")

    fun sanitize(input: String): String {
        var s = input
        s = lineCommentRegex.replace(s, "")
        s = blockCommentRegex.replace(s, "")
        s = trailingCommaRegex.replace(s, "$1")
        return s.trim()
    }
}
