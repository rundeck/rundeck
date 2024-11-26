import type { Meta, StoryObj } from "@storybook/vue3";
import "./tabview.scss";
import Tabs from "primevue/tabs";
import TabList from "primevue/tablist";
import Tab from "primevue/tab";
import TabPanels from "primevue/tabpanels";
import TabPanel from "primevue/tabpanel";

const meta: Meta<typeof Tabs> = {
  title: "Tabs",
  parameters: {
    docs: {
      componentSubtitle:
        "Tabs facilitates seamless switching between different views.",
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

type Story = StoryObj<typeof Tabs>;

export const Playground: Story = {
  render: (args) => ({
    props: Object.keys(args),
    components: { Tabs, Tab, TabList, TabPanels, TabPanel },
    setup: () => ({ args }),
    template: `<Tabs>
      <TabList>
        <Tab value="0">Header I</Tab>
        <Tab value="1">Header II</Tab>
        <Tab value="2">Header III</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="0">
          <p class="m-0">
            Content I
          </p>
        </TabPanel>
        <TabPanel value="1">
          <p class="m-0">
            Content II
          </p>
        </TabPanel>
        <TabPanel value="2">
          <p class="m-0">
            Content III
          </p>
        </TabPanel>
      </TabPanels>
    </Tabs>`,
  }),
  args: {},
};
