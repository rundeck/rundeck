package rundeck.data.util

import java.util.concurrent.TimeUnit

/**
 * @author greg
 * @since 2/23/17
 */
class Sizes {
    static final Map<String, Long> UNITS = [
            t: 1024L * 1024 * 1024 * 1024,
            g: 1024L * 1024 * 1024,
            m: 1024L * 1024,
            k: 1024L,
            b: 1L
    ]

    /**
     * Parse a file size in the form "###[tgkm][b]" (case insensitive)
     * @param size
     * @return
     */
    static Long parseFileSize(String size) {
        def m = size =~ /(\d+)((?i)[tgmk]?b?)?/
        if (m.matches()) {
            def count = m.group(1)
            def unit = m.group(2)
            def multi = unit ? UNITS[unit[0]?.toLowerCase()] ?: 1 : 1
            def value = 0
            try {
                return Long.parseLong(count) * multi
            } catch (NumberFormatException e) {
                return null
            }
        }
        null
    }

    static final Map<String, Long> TIME_UNITS = [s: 1, m: 60, h: 60 * 60, d: 24 * 60 * 60,w: 7 * 24 * 60 * 60, y: 365 * 24 * 60 * 60]
    /**
     * Return the timeout duration in seconds for a timeout string in the form "1d2h3m15s" etc
     * @param time
     * @param opts
     * @return
     */
    public static long parseTimeDuration(String time, TimeUnit unit = TimeUnit.SECONDS) {
        long timeval = 0
        def matcher = (time =~ /(\d+)(.)?/)
        matcher.each { m ->
            long val
            try {
                val = Long.parseLong(m[1])
            } catch (NumberFormatException e) {
                return
            }
            if (m[2] && TIME_UNITS[m[2]]) {
                timeval += (val * TIME_UNITS[m[2]])
            } else if (!m[2]) {
                timeval += val
            }
        }
        return unit.convert(timeval, TimeUnit.SECONDS)
    }/**
     * Return the timeout duration in seconds for a timeout string in the form "1d2h3m15s" etc
     * @param time
     * @param opts
     * @return
     */
    public static boolean validTimeDuration(String time) {
        def matcher = (time =~ /(\d+)([smhdwy])?/)
        return matcher.matches()
    }
}
