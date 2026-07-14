package org.rundeck.util.gui

/**
 * Provides canonical {@code where:} data for UI-mode data-driven Spock specs.
 *
 * <p>Usage — declare a single class-level constant and reference it in every
 * {@code where:} block so mode combinations are defined exactly once:</p>
 *
 * <pre>
 * static final UI_MODES = UiModes.defaultAndLegacy()
 *
 * def "my test"() {
 *     where:
 *     [legacyUi] {@literal <<} UI_MODES
 * }
 * </pre>
 */
class UiModes {

    /**
     * Post-promotion modes: current default first, then legacy fallback.
     * Iteration variable is typically {@code legacyUi}.
     * Use when {@code @UiModeFlag(status = PROMOTED)}.
     *
     * @return {@code [[false], [true]]}
     */
    static List<List<Object>> defaultAndLegacy() {
        [[false], [true]]
    }

    /**
     * Pre-promotion modes: new UI first, then current default.
     * Iteration variable is typically {@code nextUi}.
     * Use when {@code @UiModeFlag(status = NEXT_UI)}.
     *
     * @return {@code [[true], [false]]}
     */
    static List<List<Object>> nextUiAndDefault() {
        [[true], [false]]
    }

    /**
     * Single-mode: only the promoted default, no flag needed.
     * Use after the legacy branch has been removed.
     * Pair with {@code @UiModeFlag(status = DEPRECATED)} during the deletion grace
     * period, or no annotation at all once the feature is fully gone.
     *
     * @return {@code [[false]]}
     */
    static List<List<Object>> defaultOnly() {
        [[false]]
    }

    /**
     * Single-mode: only the legacy path is exercised. Iteration variable is
     * typically {@code legacyUi} (held at {@code true}).
     * Use when {@code @UiModeFlag(status = LEGACY)} and the spec is intentionally
     * pinned to the legacy code path (migration deferred).
     *
     * @return {@code [[true]]}
     */
    static List<List<Object>> legacyOnly() {
        [[true]]
    }
}
