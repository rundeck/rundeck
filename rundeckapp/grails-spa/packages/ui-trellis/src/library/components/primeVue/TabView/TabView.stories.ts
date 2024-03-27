import type { Meta, StoryObj } from "@storybook/vue3";
import "./tabview.scss";
import TabView from "primevue/tabview";
import TabPanel from "primevue/tabpanel";

const meta: Meta<typeof TabView> = {
  title: "TabView",
  component: TabView,
  parameters: {
    componentSubtitle: "A brief description of the TabView component",
    controls: { sort: "requiredFirst" },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {
    content: "Hello world!",
  },
};

export default meta;

type Story = StoryObj<typeof TabView>;

export const Default: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { TabView, TabPanel },
    setup() {
      return { args };
    },
    template: `<TabView v-bind="args">
      <TabPanel header="Header I">
        <p class="m-0">
          {{ args.content}}
        </p>
      </TabPanel>
      <TabPanel header="Header II">
        <p class="m-0">
          {{ args.content}} II
        </p>
      </TabPanel>
      <TabPanel header="Header III">
        <p class="m-0">
          {{ args.content}} III
        </p>
      </TabPanel>
    </TabView>`,
  }),
  args: {},
};
