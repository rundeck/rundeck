import { mount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
import { nextTick } from "vue";

import MainbarMenu from "../MainbarMenu.vue";

const simpleLinks = [
  { url: "/system/info", title: "System Info", enabled: true },
  { url: "/system/log", title: "Logs" },
  { url: "/system/config", title: "Configuration", enabled: false },
];

const nestedLinks = [
  {
    title: "Plugins",
    enabled: true,
    links: [
      { url: "/plugins/installed", title: "Installed Plugins", enabled: true },
      { url: "/plugins/repository", title: "Plugin Repository", enabled: true },
    ],
  },
  {
    title: "Configuration",
    enabled: true,
    links: [
      { url: "/config/system", title: "System Configuration", enabled: true },
      {
        url: "/config/project",
        title: "Project Configuration",
        enabled: false,
      },
    ],
  },
];

const linksWithSeparators = [
  { url: "/system/info", title: "System Info", enabled: true },
  { separator: true },
  { url: "/system/log", title: "Logs", enabled: true },
  { group: { id: "my-group" }, title: "Group Header" },
  { url: "/system/config", title: "Configuration", enabled: true },
];

const linksWithEmbeddedSeparators = [
  { url: "/system/info", title: "System Info", enabled: true },
  { url: "/system/log", title: "Logs", enabled: true, separator: true },
  {
    url: "/system/config",
    title: "Configuration",
    enabled: true,
    group: { id: "my-group" },
  },
];

const createWrapper = async (
  options: Record<string, any> = {},
): Promise<VueWrapper<any>> => {
  const props = { links: [], ...(options.props || {}) };
  const wrapper = mount(MainbarMenu, {
    ...options,
    props,
    global: {
      components: { Dropdown, Btn },
    },
  });
  await nextTick();
  return wrapper;
};

describe("MainbarMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("Data Loading", () => {
    it("should render the provided links", async () => {
      const wrapper = await createWrapper({ props: { links: simpleLinks } });

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");
    });

    it("should render no links when given an empty array", async () => {
      const wrapper = await createWrapper({ props: { links: [] } });

      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(0);
      expect(
        wrapper.findAll('[data-testid^="mainbar-submenu-toggle-"]').length,
      ).toBe(0);
    });
  });

  describe("Link Filtering and Sorting", () => {
    it("should filter out disabled links", async () => {
      const wrapper = await createWrapper({ props: { links: simpleLinks } });

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");
    });

    it("should sort links by order property if present", async () => {
      const allEnabledLinks = [
        { url: "/test/1", title: "Fifth", enabled: true, order: 100 },
        { url: "/test/2", title: "Fourth", enabled: true, order: 50 },
        { url: "/test/3", title: "First", enabled: true, order: -100 },
        { url: "/test/4", title: "Second", enabled: true },
        { url: "/test/5", title: "Third", enabled: true },
      ];

      const wrapper = await createWrapper({
        props: { links: allEnabledLinks },
      });

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(5);
      expect(renderedLinks.at(0)?.text()).toBe("First");
      expect(renderedLinks.at(1)?.text()).toBe("Second");
      expect(renderedLinks.at(2)?.text()).toBe("Third");
      expect(renderedLinks.at(3)?.text()).toBe("Fourth");
      expect(renderedLinks.at(4)?.text()).toBe("Fifth");
    });

    it("should render all links when all are enabled", async () => {
      const allEnabledLinks = [
        { url: "/system/info", title: "System Info", enabled: true },
        { url: "/system/log", title: "Logs", enabled: true },
        { url: "/system/config", title: "Configuration", enabled: true },
      ];

      const wrapper = await createWrapper({
        props: { links: allEnabledLinks },
      });

      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(3);
    });

    it("should treat links without an enabled property as enabled", async () => {
      const allEnabledLinks = [
        { url: "/system/info", title: "System Info" },
        { url: "/system/log", title: "Logs" },
        { url: "/system/config", title: "Configuration" },
      ];

      const wrapper = await createWrapper({
        props: { links: allEnabledLinks },
      });

      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(3);
    });

    it("should render no links when all are disabled", async () => {
      const allDisabledLinks = [
        { url: "/system/info", title: "System Info", enabled: false },
        { url: "/system/log", title: "Logs", enabled: false },
        { url: "/system/config", title: "Configuration", enabled: false },
      ];

      const wrapper = await createWrapper({
        props: { links: allDisabledLinks },
      });

      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(0);
    });

    it("should render no header for an empty links array without header prop", async () => {
      const wrapper = await createWrapper({ props: { links: [] } });

      expect(
        wrapper.find('[data-testid="mainbar-menu-header"]').exists(),
      ).toBe(false);
      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(0);
    });

    it("should render only the header when links is empty but header is set", async () => {
      const wrapper = await createWrapper({
        props: { links: [], header: "test" },
      });

      const header = wrapper.find('[data-testid="mainbar-menu-header"]');
      expect(header.exists()).toBe(true);
      expect(header.text()).toBe("test");
      expect(
        wrapper.findAll('[data-testid^="mainbar-menu-link-"]').length,
      ).toBe(0);
    });
  });

  describe("Link and Submenu Rendering", () => {
    it("should render simple links without header", async () => {
      const wrapper = await createWrapper({ props: { links: simpleLinks } });

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.text()).toBe("System Info");
      expect(renderedLinks.at(1)?.text()).toBe("Logs");
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");

      expect(
        wrapper.find('[data-testid="mainbar-menu-header"]').exists(),
      ).toBe(false);

      const icon = wrapper.find('[data-testid="mainbar-menu-icon"]');
      expect(icon.classes()).toEqual(
        expect.arrayContaining(["fas", "fa-cog", "fa-lg"]),
      );
    });

    it("should render simple links with header", async () => {
      const wrapper = await createWrapper({
        props: { links: simpleLinks, header: "Something" },
      });

      const header = wrapper.find('[data-testid="mainbar-menu-header"]');
      expect(header.exists()).toBe(true);
      expect(header.text()).toBe("Something");
    });

    it("should render the menu title in the toggle", async () => {
      const wrapper = await createWrapper({
        props: { links: simpleLinks, title: "Menu Title" },
      });

      const toggle = wrapper.find('[data-testid="mainbar-menu-toggle"]');
      expect(toggle.text()).toContain("Menu Title");
    });

    it("should use a custom iconCss class on the toggle icon", async () => {
      const wrapper = await createWrapper({
        props: {
          links: simpleLinks,
          header: "Something",
          iconCss: "fas fa-something fa-xs",
        },
      });

      const icon = wrapper.find('[data-testid="mainbar-menu-icon"]');
      expect(icon.classes()).toEqual(
        expect.arrayContaining(["fas", "fa-something", "fa-xs"]),
      );
    });

    it("should render links with submenus, hidden by default", async () => {
      const wrapper = await createWrapper({
        props: {
          links: [
            {
              title: "Plugins",
              enabled: true,
              links: [
                { url: "/test/1", title: "Test1", enabled: true },
                { url: "/test/2", title: "Test2", enabled: true },
              ],
            },
          ],
        },
      });

      const submenuToggle = wrapper.find(
        '[data-testid="mainbar-submenu-toggle-0"]',
      );
      expect(submenuToggle.exists()).toBe(true);
      expect(submenuToggle.text()).toContain("Plugins");
      expect(submenuToggle.attributes("href")).toBe("#");

      const child0 = wrapper.find('[data-testid="mainbar-submenu-link-0-0"]');
      const child1 = wrapper.find('[data-testid="mainbar-submenu-link-0-1"]');
      expect(child0.text()).toBe("Test1");
      expect(child1.text()).toBe("Test2");
      expect(child0.attributes("href")).toBe("/test/1");
      expect(child1.attributes("href")).toBe("/test/2");

      const submenuList = wrapper.find(
        '[data-testid="mainbar-submenu-list-0"]',
      );
      expect(submenuList.attributes("style")).toContain("display: none");
    });

    it("should render separator and group entries", async () => {
      const wrapper = await createWrapper({
        props: { links: linksWithSeparators },
      });

      const separators = wrapper.findAll(
        '[data-testid^="mainbar-menu-separator-"]',
      );
      expect(separators.length).toBe(2);

      const groupSeparator = wrapper.find('li[id="my-group"]');
      expect(groupSeparator.attributes("data-testid")).toMatch(
        /^mainbar-menu-separator-/,
      );
    });

    it("should render entries that combine url with separator/group as both", async () => {
      const wrapper = await createWrapper({
        props: { links: linksWithEmbeddedSeparators },
      });

      const separators = wrapper.findAll(
        '[data-testid^="mainbar-menu-separator-"]',
      );
      expect(separators.length).toBe(2);

      const groupSeparator = wrapper.find('li[id="my-group"]');
      expect(groupSeparator.attributes("data-testid")).toMatch(
        /^mainbar-menu-separator-/,
      );
    });

    it("should render link icons with the supplied iconCss", async () => {
      const wrapper = await createWrapper({
        props: {
          links: [
            {
              url: "/system/info",
              title: "System Info",
              enabled: true,
              iconCss: "glyphicon glyphicon-info-sign custom-class",
            },
            {
              url: "/system/log",
              title: "Logs",
              enabled: true,
              iconCss: "fas fa-list fa-lg",
            },
          ],
        },
      });

      const link0 = wrapper.find('[data-testid="mainbar-menu-link-0"]');
      const link1 = wrapper.find('[data-testid="mainbar-menu-link-1"]');
      const icon0 = link0.find("i");
      const icon1 = link1.find("i");

      expect(icon0.classes()).toEqual(
        expect.arrayContaining([
          "glyphicon",
          "glyphicon-info-sign",
          "custom-class",
        ]),
      );
      expect(icon1.classes()).toEqual(
        expect.arrayContaining(["fas", "fa-list", "fa-lg"]),
      );
    });
  });

  describe("Submenu Interaction", () => {
    it("should open a submenu when its toggle is clicked", async () => {
      const wrapper = await createWrapper({ props: { links: nestedLinks } });

      const submenuList = wrapper.find(
        '[data-testid="mainbar-submenu-list-0"]',
      );
      expect(submenuList.attributes("style")).toContain("display: none");

      await wrapper
        .find('[data-testid="mainbar-submenu-toggle-0"]')
        .trigger("click");
      await nextTick();

      const reopenedList = wrapper.find(
        '[data-testid="mainbar-submenu-list-0"]',
      );
      expect(reopenedList.attributes("style")).toContain("display: block");
      expect(reopenedList.attributes("style")).not.toContain("display: none");
    });

    it("should close an open submenu when its toggle is clicked again", async () => {
      const wrapper = await createWrapper({ props: { links: nestedLinks } });

      const toggle = wrapper.find('[data-testid="mainbar-submenu-toggle-0"]');

      await toggle.trigger("click");
      await nextTick();
      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-0"]')
          .attributes("style"),
      ).toContain("display: block");

      await toggle.trigger("click");
      await nextTick();
      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-0"]')
          .attributes("style"),
      ).toContain("display: none");
    });

    it("should close a previously open submenu when a different one is clicked", async () => {
      const wrapper = await createWrapper({ props: { links: nestedLinks } });

      await wrapper
        .find('[data-testid="mainbar-submenu-toggle-0"]')
        .trigger("click");
      await nextTick();

      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-0"]')
          .attributes("style"),
      ).toContain("display: block");
      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-1"]')
          .attributes("style"),
      ).toContain("display: none");

      await wrapper
        .find('[data-testid="mainbar-submenu-toggle-1"]')
        .trigger("click");
      await nextTick();

      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-0"]')
          .attributes("style"),
      ).toContain("display: none");
      expect(
        wrapper
          .find('[data-testid="mainbar-submenu-list-1"]')
          .attributes("style"),
      ).toContain("display: block");
    });

    it("should stop click propagation on submenu toggle", async () => {
      const wrapper = await createWrapper({ props: { links: nestedLinks } });

      const parentHandler = jest.fn();
      const submenuList = wrapper.find(
        '[data-testid="mainbar-submenu-list-0"]',
      );
      const parent = submenuList.element.parentElement;
      parent?.addEventListener("click", parentHandler);

      await wrapper
        .find('[data-testid="mainbar-submenu-toggle-0"]')
        .trigger("click");
      await nextTick();

      expect(parentHandler).not.toHaveBeenCalled();
    });
  });

  describe("Edge Cases", () => {
    it("should handle entries missing url, title, or that are null/empty", async () => {
      const incompleteLinks = [
        { title: "No URL" },
        { url: "/system/info", title: "Info" },
        { url: "/empty" },
        null,
        {},
      ];

      const wrapper = await createWrapper({
        props: { links: incompleteLinks },
      });

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/empty");
    });

    it("should render a deeply nested submenu's first level", async () => {
      const deeplyNestedLinks = [
        {
          title: "Level 1",
          enabled: true,
          links: [
            {
              title: "Level 2",
              enabled: true,
              url: "/level2",
              links: [],
            },
          ],
        },
      ];

      const wrapper = await createWrapper({
        props: { links: deeplyNestedLinks },
      });

      const submenuToggle = wrapper.find(
        '[data-testid="mainbar-submenu-toggle-0"]',
      );
      expect(submenuToggle.text()).toContain("Level 1");
    });
  });
});
