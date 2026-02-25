import PrimeVue from 'primevue/config';
import Tooltip from 'primevue/tooltip';
import Lara from '@primeuix/themes/lara';
import type { App } from 'vue';

export interface PrimeVueConfigOptions {
  includeTooltip?: boolean;
}

/**
 * Configures PrimeVue with standard Rundeck theme settings.
 * PrimeVue injects styles dynamically when this is called.
 */
export function configurePrimeVue(
  app: App,
  options: PrimeVueConfigOptions = {}
): void {

  app.use(PrimeVue, {
    theme: {
      preset: Lara,
      options: {
        prefix: 'p',
        cssLayer: true,
        darkModeSelector: '.dark',
      },
    },
  });

  if (options.includeTooltip) {
    app.directive('tooltip', Tooltip);
  }
}

export { Tooltip };
