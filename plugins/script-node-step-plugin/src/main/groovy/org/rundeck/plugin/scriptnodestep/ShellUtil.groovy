package org.rundeck.plugin.scriptnodestep

/**
 * Utility for shell Strings
 */
interface ShellUtil {
    /**
     * Append an environment line to a StringBuilder
     * @param name
     * @param value
     * @param sb
     */
    void appendEnvLine(
        String name,
        String value,
        StringBuilder sb
    )

    /**
     * Quote a string for use in a variable value
     * @param value
     * @return
     */
    String quote(final String value)
}
