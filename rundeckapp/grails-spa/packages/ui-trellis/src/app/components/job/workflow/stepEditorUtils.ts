//@ts-nocheck
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
  providerMetadata?: { glyphicon?: string; faicon?: string; fabicon?: string };
}

export interface ValidationResult {
  valid: boolean;
  errors: Record<string, string>;
}

/**
 * Returns a clean validation state.
 * Use this to reset validation before editing or after canceling edits.
 */
export function resetValidation(): ValidationResult {
  return { errors: {}, valid: true };
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
 * Plugins must already be loaded. Caller passes the step's service.
 */
export function getPluginDetailsForStep(
  element: EditStepData,
  service: string,
): PluginDetails {
  const context = getRundeckContext();
  const isJobRef = Boolean(element.jobref || element.type === "job.reference");
  const pluginName = isJobRef ? "job.reference" : element.type;

  const plugins = context.rootStore.plugins.getServicePlugins(service);
  const plugin = pluginName
    ? plugins.find((p: any) => p.name === pluginName)
    : undefined;

  if (plugin) {
    return {
      title: plugin.title || element.description || element.type || "",
      description: plugin.description || "",
      iconUrl: plugin.iconUrl || "",
      tooltip: plugin.description || "",
      ...(plugin.providerMetadata && {
        providerMetadata: plugin.providerMetadata,
      }),
    };
  }

  if (isJobRef) {
    return {
      title: "Job reference",
      description: "Execute another job",
      iconUrl: "",
      tooltip: "Execute another job",
      providerMetadata: { glyphicon: "book" },
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

  // Conditional logic: validate that all conditions have field and value
  if (step.type === "conditional.logic") {
    const conditionSet = step.config?.conditionSet || [];
    const conditionErrors: Record<string, { field?: string; value?: string }> = {};
    let hasErrors = false;

    conditionSet.forEach((conditionSet: any) => {
      if (conditionSet.conditions) {
        conditionSet.conditions.forEach((condition: any) => {
          const errors: { field?: string; value?: string } = {};

          if (!condition.field || condition.field.trim() === "") {
            errors.field = "editConditionalStep.fieldRequired";
            hasErrors = true;
          }

          if (!condition.value || condition.value.trim() === "") {
            errors.value = "editConditionalStep.valueRequired";
            hasErrors = true;
          }

          if (Object.keys(errors).length > 0) {
            conditionErrors[condition.id] = errors;
          }
        });
      }
    });

    if (hasErrors) {
      return {
        valid: false,
        errors: { conditions: conditionErrors },
      };
    }

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
