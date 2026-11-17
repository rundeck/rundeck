import type { Execution } from "../../../library/types/executions/Execution";

/**
 * Columns exposed by the EA activity table for single-column sorting. Limited to
 * values backed by persisted execution fields, since the lazy table cannot sort
 * across pages on the client.
 */
export type ActivitySortField = "id" | "dateEnded" | "user";

/**
 * PrimeVue's ascending and descending sort direction values.
 */
export type ActivitySortOrder = 1 | -1;

/**
 * Server-backed page requested by the EA activity table.
 */
export interface ActivityPageRequest {
  offset: number;
  max: number;
  sortField?: ActivitySortField;
  sortOrder?: ActivitySortOrder;
}

/**
 * A locally saved Activity query that can be selected by the EA table.
 */
export interface ActivitySavedFilter {
  filterName: string;
  query: Record<string, string>;
}

/**
 * Select option representing either no saved filter or one persisted filter.
 */
export interface ActivitySavedFilterOption {
  label: string;
  value: string;
}

/**
 * Plugin metadata needed to render an execution with its registered icon.
 */
export interface ActivityPluginDetail {
  title: string;
  iconUrl?: string;
  providerMetadata?: {
    glyphicon?: string;
    faicon?: string;
    fabicon?: string;
  };
}

/**
 * Execution enriched with the plugin presentation returned by plugin detail.
 */
export interface ActivityExecution extends Execution {
  plugin?: ActivityPluginDetail;
}
