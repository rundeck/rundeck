package org.rundeck.plugin.scriptnodestep

/**
 * Modify a script to add secre input behavior
 */
interface ScriptModifier {
    String modifyScriptForSecureInput(String script)
}
