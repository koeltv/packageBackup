import java.io.File
import java.util.function.Predicate

fun File.insertAllUntil(strings: List<String>, predicate: Predicate<String>): Int {
    var i = 0
    appendText(strings[i++])
    do {
        appendText("\n" + strings[i++])
    } while (!predicate.test(strings[i]))
    return i
}

fun String.containsAny(list: List<String>, ignoreCase: Boolean = false) = list.any { string ->
    this.contains(string, ignoreCase)
}