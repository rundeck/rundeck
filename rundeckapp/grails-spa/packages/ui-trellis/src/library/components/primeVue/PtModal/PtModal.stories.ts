import type { Meta, StoryObj } from "@storybook/vue3";

import PtModal from "./PtModal.vue";

const meta: Meta<typeof PtModal> = {
  title: "PtModal",
  component: PtModal,
  parameters: {
    componentSubtitle:
      "Confirmation-style modal built on the PrimeVue Dialog, with configurable footer buttons",
  },
  argTypes: {
    modelValue: {
      control: "boolean",
      description: "Whether the modal is open.",
    },
    header: { control: "text", description: "Title of the modal." },
    buttons: {
      control: "object",
      description:
        "Footer buttons: id, label, severity, outlined, text, icon, loading, disabled.",
    },
    closable: { control: "boolean" },
    closeOnEscape: { control: "boolean" },
    dismissableMask: { control: "boolean" },
  },
  args: {
    modelValue: true,
    header: "Discard this step?",
    buttons: [
      {
        id: "keep",
        label: "Keep editing",
        severity: "secondary",
        outlined: true,
      },
      { id: "discard", label: "Discard", severity: "danger" },
    ],
  },
};

export default meta;

type Story = StoryObj<typeof PtModal>;

export const Playground: Story = {
  name: "Playground",
  tags: ["!dev"],
  render: (args) => ({
    components: { PtModal },
    setup: () => ({ args }),
    template: `
      <PtModal v-bind="args">
        Your progress will be lost. The plugin, target nodes, and any options you've configured won't be saved.
      </PtModal>`,
  }),
};

export const Default: Story = {
  render: (args) => ({
    components: { PtModal },
    setup: () => ({ args }),
    template: `
      <PtModal v-bind="args">
        Your progress will be lost. The plugin, target nodes, and any options you've configured won't be saved.
      </PtModal>`,
  }),
};
