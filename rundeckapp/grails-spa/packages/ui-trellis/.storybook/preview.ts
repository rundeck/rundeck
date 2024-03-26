import type { Preview } from "@storybook/vue3";
import { setup } from "@storybook/vue3";

import "primevue/resources/themes/lara-light-amber/theme.css";
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
        type: "dynamic",
      },
    },
  },
};

setup((app) => {
  app.directive("tooltip", Tooltip);
});

export default preview;
