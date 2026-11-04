import type { Meta, StoryFn } from "@storybook/vue3";

import { RundeckClient, TokenCredentialProvider } from "@rundeck/client";
import { BrowserFetchHttpClient } from "@azure/ms-rest-js/es/lib/browserFetchHttpClient";

import "../../../stories/setup";

import { RootStore } from "../../../stores/RootStore";

import WebhookSelect from "./WebhookSelect.vue";

window._rundeck.rundeckClient = new RundeckClient(
  new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN),
  {
    baseUri: process.env.STORYBOOK_RUNDECK_URL,
    httpClient: new BrowserFetchHttpClient(),
  },
);

export default {
  title: "Widgets/Webhook Select",
  component: WebhookSelect,
} as Meta<typeof WebhookSelect>;

export const pluginPicker: StoryFn<typeof WebhookSelect> = () => {
  const rootStore = new RootStore(window._rundeck.rundeckClient);
  window._rundeck.rootStore = rootStore;
  return {
    template: `<WebhookSelect project="Test" @item:selected="() => {}"/>`,
    provide: { rootStore },
    components: { WebhookSelect },
    data: () => ({
      project: "Test",
      selected: "",
    }),
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
