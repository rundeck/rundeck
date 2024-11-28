import type { Meta, StoryObj } from "@storybook/vue3";
import "./tabs.scss";
import Tabs from "primevue/tabs";
import TabList from "primevue/tablist";
import Tab from "primevue/tab";
import TabPanels from "primevue/tabpanels";
import TabPanel from "primevue/tabpanel";

const meta: Meta<typeof Tabs> = {
  title: "Tabs",
  parameters: {
    componentSubtitle:
      "Tabs facilitates seamless switching between different views.",
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
    template: `<Tabs :value="args.value">
      <TabList>
        <Tab value="0">Header I</Tab>
        <Tab value="1">Header II</Tab>
        <Tab value="2">Header III</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="0">
          <p>
            Content I
          </p>
        </TabPanel>
        <TabPanel value="1">
          <p>
            Content II
          </p>
        </TabPanel>
        <TabPanel value="2">
          <p>
            Content III
          </p>
        </TabPanel>
      </TabPanels>
    </Tabs>`,
  }),
  args: {
    value: "0",
  },
};
