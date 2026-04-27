package org.rundeck.util.annotations

/**
 * Lifecycle status of a UI-mode flag feature being tested.
 */
enum UiModeStatus {
    /** Feature is behind nextUi=true; the old path is still the default. */
    NEXT_UI,

    /** Feature is now the default; the old path is accessible via legacyUi=true. */
    PROMOTED,

    /** Old path only remains; the new path has been fully adopted. To be removed. */
    LEGACY
}
