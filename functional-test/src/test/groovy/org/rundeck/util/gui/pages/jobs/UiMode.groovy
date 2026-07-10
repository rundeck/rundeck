package org.rundeck.util.gui.pages.jobs

/**
 * Identifies the UI mode to use when loading a job edit path.
 * Replaces the confusing boolean overloads on {@link JobCreatePage#loadEditPath}.
 */
enum UiMode {
    /** No UI flag — use the current default UI. */
    DEFAULT,

    /** Activate the new UI via {@code ?nextUi=true}. */
    NEXT_UI,

    /** Fall back to the legacy UI via {@code ?legacyUi=true}. */
    LEGACY
}
