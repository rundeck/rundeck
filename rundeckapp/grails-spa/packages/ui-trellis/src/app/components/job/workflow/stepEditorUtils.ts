/**
 * Shared utility functions for step editing.
 * Used by both WorkflowSteps (root level) and InnerStepList (inner level).
 */
import { mkid } from "./types/workflowFuncs";
import type { EditStepData, JobRefDefinition } from "./types/workflowTypes";
import { getRundeckContext } from "../../../../library";
import { ServiceType, type Plugin } from "../../../../library/stores/Plugins";
import { validatePluginConfig } from "../../../../library/modules/pluginService";

export interface PluginDetails {
  title: string;
  description: string;
  iconUrl: string;
  tooltip: string;
  providerMetadata?: { glyphicon?: string; faicon?: string; fabicon?: string };
}

/** Field name -> error message, as returned by validatePluginConfig. */
export type FieldValidationErrors = Record<string, string>;

export interface ValidationResult {
  valid: boolean;
  /**
   * Field-level errors (jobref, plugin config fields, `_general`), plus an
   * optional nested `errorhandler` map for error-handler-specific field errors.
   */
  errors: Record<string, string | FieldValidationErrors>;
}

/**
 * Returns a clean validation state.
 * Use this to reset validation before editing or after canceling edits.
 */
export function resetValidation(): ValidationResult {
  return { errors: {}, valid: true };
}

/**
 * Default jobref fields for job-reference step editors.
 * Shared by JobRefForm and EditStepCard so defaults stay in sync.
 */
export function createJobRefDefinition(
  projectName: string = getRundeckContext().projectName,
): JobRefDefinition {
  return {
    nodeStep: false,
    name: "",
    uuid: "",
    group: "",
    project: projectName,
    args: "",
    failOnDisable: false,
    childNodes: false,
    importOptions: false,
    ignoreNotifications: false,
    useName: false,
    nodefilters: {
      filter: "",
      dispatch: {
        threadcount: undefined,
        keepgoing: undefined,
        rankAttribute: undefined,
        rankOrder: undefined,
        nodeIntersect: undefined,
      },
    },
  };
}

/**
 * Looks up plugin metadata by name for a given service. Plugins must
 * already be loaded. Shared by `resolveStepTypeTitle` and
 * `getPluginDetailsForStep` so both use one source of truth for the lookup.
 */
function findStepPlugin(
  service: string,
  pluginName: string,
): Plugin | undefined {
  const plugins =
    getRundeckContext().rootStore.plugins.getServicePlugins(service);
  return plugins.find((p: Plugin) => p.name === pluginName);
}

/**
 * Resolves a human-readable type title for a step provider (e.g. "Command",
 * "Job Reference"), based on loaded plugin metadata. Falls back to the raw
 * provider name when no plugin metadata is available yet.
 */
export function resolveStepTypeTitle(
  service: string,
  provider: string,
): string {
  const plugin = findStepPlugin(service, provider);

  if (plugin?.title) {
    return plugin.title;
  }

  return provider === "job.reference" ? "Job reference" : provider;
}

/**
 * Creates a new step object from a provider selection.
 * Handles job references and regular plugins.
 *
 * The returned step includes a unique `id`, the correct `nodeStep` flag
 * based on the service type, and a default "Step Name" (`description`)
 * derived from the step's type. Callers are responsible for adding the step
 * to their commands array and managing editing state.
 */
export function createStepFromProvider(
  service: string,
  provider: string,
): EditStepData {
  const nodeStep = service === ServiceType.WorkflowNodeStep;
  const description = resolveStepTypeTitle(service, provider);

  if (provider === "job.reference") {
    return {
      type: provider,
      description,
      nodeStep,
      jobref: {
        ...createJobRefDefinition(),
        nodeStep,
      },
      id: mkid(),
    } as EditStepData;
  }

  // Regular plugin step
  return {
    type: provider,
    description,
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
  const isJobRef = Boolean(element.jobref || element.type === "job.reference");
  const pluginName = isJobRef ? "job.reference" : element.type;
  const plugin = findStepPlugin(service, pluginName);

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

  // Regular plugin - call API validation
  try {
    const response = await validatePluginConfig(
      serviceName,
      step.type!,
      step.config || {},
    );

    if (!response.valid || Object.keys(response.errors || {}).length > 0) {
      return {
        valid: false,
        errors: response.errors || {},
      };
    }
  } catch (e) {
    console.error("Error validating plugin config:", e);
    return {
      valid: false,
      errors: { _general: "Validation failed due to an error" },
    };
  }

  if (!step.errorhandler?.type || step.errorhandler.jobref) {
    return { valid: true, errors: {} };
  }

  const errorHandlerService = step.errorhandler.nodeStep
    ? ServiceType.WorkflowNodeStep
    : ServiceType.WorkflowStep;

  try {
    const errorHandlerResponse = await validatePluginConfig(
      errorHandlerService,
      step.errorhandler.type,
      step.errorhandler.config || {},
    );

    if (
      errorHandlerResponse.valid &&
      Object.keys(errorHandlerResponse.errors || {}).length === 0
    ) {
      return { valid: true, errors: {} };
    }

    return {
      valid: false,
      errors: { errorhandler: errorHandlerResponse.errors || {} },
    };
  } catch (e) {
    console.error("Error validating error handler config:", e);
    return {
      valid: false,
      errors: {
        errorhandler: { _general: "Validation failed due to an error" },
      },
    };
  }
}
