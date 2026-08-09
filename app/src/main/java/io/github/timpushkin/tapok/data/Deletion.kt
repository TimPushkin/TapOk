package io.github.timpushkin.tapok.data

enum class Deletion {
    /**
     * Can be undone.
     */
    SOFT,

    /**
     * Cannot be undone, applied only if previously deleted with [SOFT].
     */
    HARD_AFTER_SOFT,

    /**
     * Cannot be undone
     */
    HARD
}
