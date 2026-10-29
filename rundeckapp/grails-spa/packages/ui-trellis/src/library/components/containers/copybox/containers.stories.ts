import type { Meta, StoryFn } from "@storybook/vue3";

import CopyBox from "./CopyBox.vue";

export default {
  title: "Containers",
  component: CopyBox,
} as Meta<typeof CopyBox>;

function setupStory(vue: { $el: unknown }) {
  if (!(vue.$el instanceof HTMLElement) || !vue.$el.parentElement) {
    return;
  }
  const parent = vue.$el.parentElement;
  parent.style.height = "100vh";
  parent.style.overflow = "hidden";
  parent.style.position = "relative";
  parent.style.padding = "20px";
  document.body.style.overflow = "hidden";
}

export const copyBox: StoryFn<typeof CopyBox> = (args) => {
  return {
    setup() {
      return { args };
    },
    template: `
        <CopyBox v-bind="args"/>`,
    components: { CopyBox },
    mounted() {
      setupStory(this);
    },
    methods: {},
  };
};
copyBox.args = {
  content: "Text to copy!",
};

export const copyBoxLongContent: StoryFn<typeof CopyBox> = (args) => {
  return {
    setup() {
      return { args };
    },
    template: `
        <CopyBox v-bind="args"/>`,
    components: { CopyBox },
    mounted() {
      setupStory(this);
    },
    methods: {},
  };
};
copyBoxLongContent.args = {
  content:
    "http://localhost:8080/api/40/webhook/OSUK4zGjC9jx1Dhfdi9SYaaADUTBBhuZ#Generic",
};
