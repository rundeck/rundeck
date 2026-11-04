import type { Meta, StoryFn } from "@storybook/vue3";
// import { addons } from "@storybook/addons";

import "../../../stories/setup";

import Drawer from "./Drawer.vue";

export default {
  title: "Containers/Drawer",
  component: Drawer,
} as Meta<typeof Drawer>;

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

export const drawer: StoryFn<typeof Drawer> = (args) => {
  // const chan = addons.getChannel();

  return {
    setup() {
      return { args };
    },
    template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="args" @close="close">Foo</Drawer>
        </div>`,
    components: { Drawer },
    mounted() {
      setupStory(this);
    },
    methods: {
      close() {
        // chan.emit("Open", false);
        args.visible = false;
      },
    },
  };
};
drawer.args = {
  title: "Settings",
  visible: true,
  placement: "left",
};

export const autoSize: StoryFn<typeof Drawer> = (args) => {
  // const chan = addons.getChannel();

  return {
    setup() {
      return { args };
    },
    template: `
        <div style="height: 100%;width: 100%;background-color: beige;position: relative;overflow: hidden;">
            <Drawer v-bind="args" @close="close">Foo</Drawer>
        </div>`,
    components: { Drawer },
    mounted() {
      setupStory(this);
    },
    methods: {
      close() {
        // chan.emit("Open", false);
        args.visible = false;
      },
    },
  };
};
autoSize.args = {
  title: "Settings",
  visible: true,
  width: "50%",
  height: "50%",
  placement: "left",
};
