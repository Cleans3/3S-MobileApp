package shelftap.android.session

/** Monotonic idle clock for auto-logout. Time keeps running while the app is stopped. */
class InactivityTracker(private val limitMs: Long, private val clock: () -> Long) {
    @Volatile
    private var lastInteraction = clock()

    fun touch() {
        lastInteraction = clock()
    }

    fun expired(): Boolean = clock() - lastInteraction >= limitMs

    fun remainingMs(): Long = (limitMs - (clock() - lastInteraction)).coerceAtLeast(0)
}
