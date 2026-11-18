import PrimeVue from "primevue/config";
import Tooltip from "primevue/tooltip";
import Lara from "@primeuix/themes/lara";
import { createApp, type App } from "vue";

export interface PrimeVueConfigOptions {
  includeTooltip?: boolean;
}

/** Standard Rundeck PrimeVue theme. */
const RUNDECK_PRIMEVUE_THEME = {
  preset: Lara,
  options: {
    prefix: "p",
    cssLayer: true,
    darkModeSelector: ".dark",
  },
};

/**
 * Registers the Rundeck theme with this bundle's PrimeVue theme registry
 * without creating a Vue app.
 *
 * Each page entry has its own webpack runtime, so it gets its own copy of the
 * theme registry. An entry whose PrimeVue components are rendered by another
 * entry's app (e.g. widgets added to the UI socket) must call this, or those
 * components render without their theme variables.
 */
export function registerPrimeVueTheme(): void {
  // Installing PrimeVue on an unmounted app runs its own theme setup against
  // this bundle's registry; the app itself is never mounted.
  configurePrimeVue(createApp({}));
}

/**
 * Configures PrimeVue with standard Rundeck theme settings.
 * PrimeVue injects styles dynamically when this is called.
 */
export function configurePrimeVue(
  app: App,
  options: PrimeVueConfigOptions = {},
): void {
  app.use(PrimeVue, { theme: RUNDECK_PRIMEVUE_THEME });

  if (options.includeTooltip) {
    app.directive("tooltip", Tooltip);
  }
}

export { Tooltip };
