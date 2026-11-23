import type { Meta, StoryObj } from "@storybook/vue3";
import Column from "primevue/column";
import PtDataTable from "./PtDataTable.vue";

const meta: Meta<typeof PtDataTable> = {
  title: "PrimeVue/PtDataTable",
  component: PtDataTable,
};

export default meta;
type Story = StoryObj<typeof PtDataTable>;

export const Default: Story = {
  render: (args) => ({
    components: { Column, PtDataTable },
    setup() {
      return { args };
    },
    template: `
      <PtDataTable v-bind="args">
        <Column field="id" header="Execution" />
        <Column field="executor" header="Executor" />
      </PtDataTable>
    `,
  }),
  args: {
    value: [
      { id: "#123", executor: "admin" },
      { id: "#124", executor: "ops" },
    ],
    dataKey: "id",
  },
};
