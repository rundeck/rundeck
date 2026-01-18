// @ts-nocheck
import type { Meta, StoryObj } from "@storybook/vue3";
import EditConditionalStepCard from "./EditConditionalStepCard.vue";
import mitt from "mitt";
import { RootStore } from "@/library/stores/RootStore";
import { RundeckToken } from "@/library/interfaces/rundeckWindow";

const eventBus = mitt();

const meta: Meta<typeof EditConditionalStepCard> = {
  title: "App/EditConditionalStepCard",
  component: EditConditionalStepCard,
  parameters: {
    componentSubtitle: "Edit panel for Conditional Logic Node Step",
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
    layout: "padded",
  },
  async beforeEach() {
    const rootStore = new RootStore(window._rundeck?.rundeckClient);

    window._rundeck = {
      rdBase: "http://localhost:4440",
      apiVersion: "52",
      projectName: "TestProject.rdproject",
      appMeta: {},
      token: {} as RundeckToken,
      tokens: {},
      navbar: { items: [] as any },
      feature: {},
      data: {},
      rootStore: rootStore,
      eventBus: eventBus,
    };
  },
};

export default meta;

type Story = StoryObj<typeof EditConditionalStepCard>;

export const Default: Story = {
  name: "Default",
  render: (args) => ({
    components: { EditConditionalStepCard },
    setup: () => ({ args }),
    template: `
      <div style="max-width: 1300px; padding: 20px; background: #f5f5f5;">
        <EditConditionalStepCard v-bind="args" />
      </div>
    `,
  }),
  args: {},
};

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { EditConditionalStepCard },
    setup: () => ({ args }),
    template: `
      <div style="max-width: 1300px; padding: 20px; background: #f5f5f5;">
        <EditConditionalStepCard v-bind="args" />
      </div>
    `,
  }),
  args: {},
};
