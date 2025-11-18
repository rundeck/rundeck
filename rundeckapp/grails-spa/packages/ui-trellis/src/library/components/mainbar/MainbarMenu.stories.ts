import { Story } from "@storybook/blocks";
import type { Meta, StoryObj } from "@storybook/vue3";
import MainbarMenu from "./MainbarMenu.vue";
import { setup } from "@storybook/vue3";
import * as uiv from "uiv";

// Setup UIV components that MainbarMenu depends on
setup((app) => {
  app.use(uiv);
});

// Define the metadata for the component
const meta: Meta<typeof MainbarMenu> = {
  title: "App/MainBar/MainbarMenu",
  component: MainbarMenu,
  parameters: {
    componentSubtitle: "Mainbar Dropdown Menu",
    actions: {
      disable: false, // Enable action logging for click events
    },
    controls: {
      disable: false,
    },
    mockData: [],
  },
  argTypes: {
    links: {
      control: {
        type: "object",
      },
      description: "Navigation links for the Mainbar menu",
    },
    title: {
      control: {
        type: "text",
      },
      description: "Title of the menu (optional)",
    },
  },
  args: {
    links: [],
    title: "",
    header: "",
    iconCss: "fas fa-cog fa-lg",
  },
};

export default meta;

type Story = StoryObj<typeof MainbarMenu>;

// Helper function to generate a template
const generateTemplate = (args: Record<string, any>) => {
  return `<div style="height: 300px; padding-top: 200px; display: flex; justify-content: center;">
    <MainbarMenu v-bind="args" />
  </div>`;
};

// Basic playground story
export const Playground: Story = {
  name: "Playground",
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: `<div style="height: 300px; padding-top: 200px; display: flex; justify-content: center;">
      <MainbarMenu v-bind="args"/>
    </div>`,
  }),
  args: {
    links: [
      { url: "/system/info", title: "System Info", enabled: true },
      { url: "/system/log", title: "Logs", enabled: true },
      { url: "/system/config", title: "Configuration", enabled: true },
    ],
  },
};

// Story with simple links
export const SystemConfigMenu: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    header: "System",
    links: [
      { url: "/system/info", title: "System Info", enabled: true },
      { url: "/system/log", title: "Logs", enabled: true },
      { url: "/system/config", title: "Configuration", enabled: true },
    ],
  },
};
// Story with simple links
export const UserMenu: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    iconCss: "fas fa-user fa-lg",
    subHeader: "Hi, username!",
    header: "",
    links: [
      {
        url: "/user/profile",
        title: "Profile",
        enabled: true,
        separator: true,
      },
      { url: "/user/logout", title: "Logout", enabled: true },
    ],
  },
};

// Story with nested links (submenus)
export const NestedLinks: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    links: [
      {
        title: "Plugins",
        enabled: true,
        links: [
          {
            url: "/plugins/installed",
            title: "Installed Plugins",
            enabled: true,
          },
          {
            url: "/plugins/repository",
            title: "Plugin Repository",
            enabled: true,
          },
        ],
      },
      {
        title: "Configuration",
        enabled: true,
        links: [
          {
            url: "/config/system",
            title: "Mainbar",
            enabled: true,
          },
          {
            url: "/config/project",
            title: "Project Configuration",
            enabled: true,
          },
        ],
      },
    ],
  },
};

// Story with separators
export const LinksWithSeparators: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    links: [
      { url: "/system/info", title: "System Info", enabled: true },
      { separator: true },
      { url: "/system/log", title: "Logs", enabled: true },
      { group: { id: "my-group" }, title: "Group Header" },
      { url: "/system/config", title: "Configuration", enabled: true },
    ],
  },
};

// Story with icons
export const LinksWithIcons: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    links: [
      {
        url: "/system/info",
        title: "System Info",
        enabled: true,
        icon: "info-sign",
        iconCss: "custom-class",
      },
      {
        url: "/system/log",
        title: "Logs",
        enabled: true,
        icon: "list",
      },
      {
        url: "/system/config",
        title: "Configuration",
        enabled: true,
        icon: "cog",
      },
    ],
  },
};

// Story with mixed links (some enabled, some disabled)
export const MixedEnabledLinks: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    links: [
      { url: "/system/info", title: "System Info", enabled: true },
      { url: "/system/log", title: "Logs", enabled: true },
      { url: "/system/config", title: "Configuration", enabled: false }, // This one should not appear
      { url: "/system/admin", title: "Administration", enabled: true },
    ],
  },
};

// Story with complex structure
export const ComplexStructure: Story = {
  render: (args) => ({
    components: { MainbarMenu },
    setup() {
      return { args };
    },
    template: generateTemplate(args),
  }),
  args: {
    links: [
      {
        url: "/system/info",
        title: "System Info",
        enabled: true,
        icon: "info-sign",
      },
      { separator: true },
      {
        title: "Plugins",
        enabled: true,
        icon: "puzzle-piece",
        links: [
          {
            url: "/plugins/installed",
            title: "Installed Plugins",
            enabled: true,
          },
          {
            url: "/plugins/repository",
            title: "Plugin Repository",
            enabled: true,
          },
        ],
      },
      { url: "/system/log", title: "Logs", enabled: true, icon: "list" },
      { group: { id: "admin-section" }, title: "Administration" },
      {
        title: "Administration",
        enabled: true,
        icon: "wrench",
        links: [
          { url: "/admin/users", title: "Users", enabled: true },
          { url: "/admin/roles", title: "Roles", enabled: true },
          { url: "/admin/settings", title: "Settings", enabled: false }, // This should not appear in submenu
        ],
      },
      {
        url: "/system/config",
        title: "Configuration",
        enabled: true,
        icon: "cog",
      },
    ],
  },
};
