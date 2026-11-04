package com.server.croniot.ota

// The single-range subset of RFC 9110 §14.1.2 a resuming download sends:
// "bytes=START-" or "bytes=START-END". Anything else is served whole.
data class ByteRange(val start: Long, val endInclusive: Long) {
    val length: Long get() = endInclusive - start + 1

    companion object {
        sealed class Parsed {
            object Whole : Parsed()
            data class Partial(val range: ByteRange) : Parsed()
            object Unsatisfiable : Parsed()
        }

        fun parse(header: String?, totalSize: Long): Parsed {
            if (header == null) return Parsed.Whole
            val spec = header.trim().removePrefix("bytes=").takeIf { it != header.trim() } ?: return Parsed.Whole
            if (',' in spec) return Parsed.Whole
            val dash = spec.indexOf('-')
            if (dash <= 0) return Parsed.Whole // also "-N" suffix ranges: not used by devices
            val start = spec.substring(0, dash).trim().toLongOrNull() ?: return Parsed.Whole
            val endText = spec.substring(dash + 1).trim()
            val end = if (endText.isEmpty()) totalSize - 1 else endText.toLongOrNull() ?: return Parsed.Whole
            if (start >= totalSize || end < start) return Parsed.Unsatisfiable
            return Parsed.Partial(ByteRange(start, minOf(end, totalSize - 1)))
        }
    }
}
