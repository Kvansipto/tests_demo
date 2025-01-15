package utils


fun wait(i: Int) {
    try {
        Thread.sleep(i.toLong())
    } catch (var2: InterruptedException) {
        var2.printStackTrace()
    }
}

inline fun <O> ignoreError(block: () -> O): O? = try {
    block()
} catch (ex: Throwable) {
    when (ex) {
        is InterruptedProcessError -> throw ex
        else -> null
    }
}

class InterruptedProcessError(text: String) : AssertionError(text)