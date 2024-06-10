import type { Meta, StoryObj } from "@storybook/vue3";
import "./tabview.scss";
import TabView from "primevue/tabview";
import TabPanel from "primevue/tabpanel";

const meta: Meta<typeof TabView> = {
  title: "TabView",
  parameters: {
    docs: {
      componentSubtitle:
        "TabView is a container component to group content with tabs.",
    },
    actions: {
      disable: true,
    },
    controls: {
      disable: true,
    },
  },
  // TODO: Replace these args with ones appropriate for the component you are building.
  args: {},
};

export default meta;

type Story = StoryObj<typeof TabView>;

export const Playground: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { TabView, TabPanel },
    setup: () => ({ args }),
    template: `<TabView>
      <TabPanel header="Header I">
        <p class="m-0">
          Content I
        </p>
      </TabPanel>
      <TabPanel header="Header II">
        <p class="m-0">
          Content II
        </p>
      </TabPanel>
      <TabPanel header="Header III">
        <p class="m-0">
          Content III
        </p>
      </TabPanel>
    </TabView>`,
  }),
  args: {},
};
