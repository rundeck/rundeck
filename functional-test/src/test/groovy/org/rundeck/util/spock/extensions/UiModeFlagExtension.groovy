package org.rundeck.util.spock.extensions

import org.rundeck.util.annotations.UiModeFlag
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

/**
 * Silent, observational-only Spock global extension for {@link UiModeFlag}.
 *
 * <p>On spec load, attaches the annotation's metadata to the {@link SpecInfo} tag map
 * so downstream tooling (test-report augmentation, {@code reportUiFlags} task) can
 * consume it. This extension intentionally:</p>
 * <ul>
 *   <li>Never skips a spec</li>
 *   <li>Never fails a test</li>
 *   <li>Never logs anything under normal conditions</li>
 *   <li>Swallows all exceptions (fail-soft) — a broken annotation never breaks a run</li>
 * </ul>
 *
 * <p>Pass {@code -DuiModeFlagVerbose=true} to print a single-line startup summary
 * listing all annotated specs found during the run (interactive debugging only).</p>
 */
class UiModeFlagExtension implements IGlobalExtension {

    private static final boolean VERBOSE = Boolean.getBoolean('uiModeFlagVerbose')
    private final List<String> annotatedSpecs = []

    @Override
    void visitSpec(SpecInfo spec) {
        try {
            UiModeFlag annotation = spec.reflection.getAnnotation(UiModeFlag)
            if (annotation == null) return

            spec.addTag("uiModeFlag.featureName:${annotation.featureName()}")
            spec.addTag("uiModeFlag.status:${annotation.status()}")
            spec.addTag("uiModeFlag.mechanism:${annotation.mechanism()}")

            if (VERBOSE) {
                annotatedSpecs << "${spec.name} [${annotation.featureName()} / ${annotation.status()} / ${annotation.mechanism()}]"
            }
        } catch (Exception ignored) {
            // Fail-soft: annotation errors must never abort a test run
        }
    }

    @Override
    void start() {
        // No-op — logging deferred to stop() so the list is complete
    }

    @Override
    void stop() {
        if (VERBOSE && annotatedSpecs) {
            System.out.println("[UiModeFlagExtension] ${annotatedSpecs.size()} annotated spec(s): ${annotatedSpecs.join(', ')}")
        }
    }
}
