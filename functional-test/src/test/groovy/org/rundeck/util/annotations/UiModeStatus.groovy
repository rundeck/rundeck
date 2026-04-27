package org.rundeck.util.annotations

/**
 * Lifecycle status of a UI-mode flag feature being tested.
 *
 * <p>Four-state lifecycle:</p>
 * <pre>
 *   NEXT_UI ──► PROMOTED ──► LEGACY ──► DEPRECATED ──► [removed]
 * </pre>
 *
 * <p>The {@code description} field on {@link UiModeFlag} should be used to add
 * any human context — especially for {@link #LEGACY} specs, where the reason
 * for the deferred migration belongs in the annotation.</p>
 */
enum UiModeStatus {
    /**
     * Feature is behind {@code nextUi=true}; the old path is still the default.
     * Iteration helper: {@link org.rundeck.util.gui.UiModes#nextUiAndDefault()}.
     */
    NEXT_UI,

    /**
     * Feature is now the default; the old path is accessible via {@code legacyUi=true}.
     * Iteration helper: {@link org.rundeck.util.gui.UiModes#defaultAndLegacy()}.
     */
    PROMOTED,

    /**
     * Spec is intentionally pinned to the legacy code path; promotion deferred
     * (e.g. dependent UI not yet shipped). The new default still exists in product;
     * this spec just doesn't exercise it yet.
     * Iteration helper: {@link org.rundeck.util.gui.UiModes#legacyOnly()}.
     */
    LEGACY,

    /**
     * Legacy branch is scheduled for removal — only the default is exercised.
     * Use as a tombstone state: signals to reviewers that any leftover
     * {@code if (legacyUi)} branches in product / page objects are removable.
     * Iteration helper: {@link org.rundeck.util.gui.UiModes#defaultOnly()}.
     * Once the legacy code is actually deleted, drop the annotation entirely.
     */
    DEPRECATED
}
