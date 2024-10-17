import type { Preview } from "@storybook/vue3";
import { setup } from "@storybook/vue3";
import PrimeVue from "primevue/config";
import Lara from "@primevue/themes/lara";

import "../src/library/theme/tokens.css";
import "./storybook.css";

import Tooltip from "primevue/tooltip";

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    docs: {
      toc: {
        headingSelector: "h2, h3",
        ignoreSelector: "h2.sbdocs-subtitle",
        title: "Jump to Section",
        disable: false,
      },
      source: {
        type: "auto",
      },
    },
  },
};

setup((app) => {
  app.directive("tooltip", Tooltip);
  app.use(PrimeVue, {
    theme: {
      preset: Lara,
    },
  });
});

export default preview;
