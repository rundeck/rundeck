/**
 * Shared utility functions for step editing.
 * Used by both WorkflowSteps (root level) and InnerStepList (inner level).
 */
import { mkid } from "./types/workflowFuncs";
import type { EditStepData } from "./types/workflowTypes";
import { getRundeckContext } from "../../../../library";
import { ServiceType } from "../../../../library/stores/Plugins";
import { validatePluginConfig } from "../../../../library/modules/pluginService";

export interface PluginDetails {
  title: string;
  description: string;
  iconUrl: string;
  tooltip: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: Record<string, string>;
}

/**
 * Creates a new step object from a provider selection.
 * Handles job references, conditional logic, and regular plugins.
 *
 * The returned step includes a unique `id` and the correct `nodeStep` flag
 * based on the service type. Callers are responsible for adding the step
 * to their commands array and managing editing state.
 */
export function createStepFromProvider(
  service: string,
  provider: string,
): EditStepData {
  const nodeStep = service === ServiceType.WorkflowNodeStep;

  if (provider === "job.reference") {
    return {
      type: provider,
      description: "",
      nodeStep,
      jobref: {
        nodeStep,
      },
      id: mkid(),
    } as EditStepData;
  }

  if (provider === "conditional.logic") {
    return {
      type: provider,
      config: {},
      nodeStep,
      description: "",
      id: mkid(),
    } as EditStepData;
  }

  // Regular plugin step
  return {
    type: provider,
    config: {},
    nodeStep,
    id: mkid(),
  } as EditStepData;
}

/**
 * Looks up plugin metadata from rootStore for a given step element.
 * Plugins must already be loaded in the rootStore (via `rootStore.plugins.load()`)
 * before calling this function.
 */
export function getPluginDetailsForStep(element: EditStepData): PluginDetails {
  const context = getRundeckContext();
  const plugins = element.nodeStep
    ? context.rootStore.plugins.getServicePlugins(ServiceType.WorkflowNodeStep)
    : context.rootStore.plugins.getServicePlugins(ServiceType.WorkflowStep);

  const plugin = plugins.find((p: any) => p.name === element.type);

  if (plugin) {
    return {
      title: plugin.title || element.description || element.type || "",
      description: plugin.description || "",
      iconUrl: plugin.iconUrl || "",
      tooltip: plugin.description || "",
    };
  }

  return {
    title: element.description || element.type || "",
    description: "",
    iconUrl: "",
    tooltip: "",
  };
}

/**
 * Validates a step for save.
 *
 * - Job references: validates that name or uuid is present.
 *   Returns `errors.jobref` with a key string that callers can use for i18n.
 * - Conditional logic: skips validation (always valid).
 * - Regular plugins: calls the `validatePluginConfig` API endpoint.
 */
export async function validateStepForSave(
  step: EditStepData,
  serviceName: string,
): Promise<ValidationResult> {
  // Job reference validation
  if (step.jobref) {
    if (!step.jobref.name && !step.jobref.uuid) {
      return {
        valid: false,
        errors: { jobref: "commandExec.jobName.blank.message" },
      };
    }
    return { valid: true, errors: {} };
  }

  // Conditional logic steps skip plugin validation
  if (step.type === "conditional.logic") {
    return { valid: true, errors: {} };
  }

  // Regular plugin - call API validation
  try {
    const response = await validatePluginConfig(
      serviceName,
      step.type!,
      step.config || {},
    );

    if (response.valid && Object.keys(response.errors || {}).length === 0) {
      return { valid: true, errors: {} };
    }

    return {
      valid: false,
      errors: response.errors || {},
    };
  } catch (e) {
    console.error("Error validating plugin config:", e);
    return {
      valid: false,
      errors: { _general: "Validation failed due to an error" },
    };
  }
}
