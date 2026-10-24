import type { Meta, StoryFn } from "@storybook/vue3";

import { Rundeck, TokenCredentialProvider } from "@rundeck/client";
import { BrowserFetchHttpClient } from "@azure/ms-rest-js/es/lib/browserFetchHttpClient";

import "../../../stories/setup";

import { RootStore } from "../../../stores/RootStore";

import ProjectSelect from "./ProjectSelect.vue";

window._rundeck.rundeckClient = new Rundeck(
  new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN),
  {
    baseUri: process.env.STORYBOOK_RUNDECK_URL,
    httpClient: new BrowserFetchHttpClient(),
  },
);

export default {
  title: "Widgets/Project Select",
  component: ProjectSelect,
} as Meta<typeof ProjectSelect>;

export const projectPicker: StoryFn<typeof ProjectSelect> = () => {
  const rootStore = new RootStore(window._rundeck.rundeckClient);

  return {
    template: `<ProjectSelect />`,
    provide: { rootStore },
    components: { ProjectSelect },
    mounted() {
      const el = this.$el as HTMLElement;
      el.parentElement!.style.height = "100vh";
      el.parentElement!.style.overflow = "hidden";
      el.parentElement!.style.position = "relative";
      el.parentElement!.style.padding = "20px";
      document.body.style.overflow = "hidden";
    },
  };
};
