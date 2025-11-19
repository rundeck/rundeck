import { mount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
import { nextTick } from "vue";

import MainbarMenu from "../MainbarMenu.vue";

// Test fixtures
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

const linksWithIcons = [
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
];

// Test fixtures with various link structures

describe("MainbarMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const mountMainbarMenu = async (
    options?: Record<string, any>,
  ): Promise<VueWrapper<any>> => {
    const wrapper = mount(MainbarMenu, {
      ...options,
      global: {
        components: {
          Dropdown,
          Btn,
        },
      },
    });

    // Wait for component to mount and update
    await nextTick();

    return wrapper;
  };

  // Test Case 1: Data Loading
  describe("Data Loading", () => {
    it("should load and parse valid JSON data from DOM", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: simpleLinks },
      });

      // Verify the data was loaded correctly
      expect(wrapper.vm.links).toEqual(simpleLinks);
    });

    it("should handle case when DOM element does not exist or contains invalid data", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({ props: { links: [] } });

      // Verify the links array is empty
      expect(wrapper.vm.links).toEqual([]);

      // Verify that no links are displayed
      expect(
        wrapper.findAll('li:not(.dropdown-header):not([role="separator"])'),
      ).toHaveLength(0);
    });
  });

  // Test Case 2: Link Filtering and Sorting
  describe("Link Filtering and Sorting", () => {
    it("should filter out disabled links", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: simpleLinks },
      });

      // Verify the computed property filters out disabled links
      expect(wrapper.vm.enabledLinks).toBeDefined();
      expect(wrapper.vm.enabledLinks.length).toBe(2); // Only 2 of 3 are enabled
      expect(
        wrapper.vm.enabledLinks.every((link) => link.enabled !== false),
      ).toBe(true);

      // Verify that only enabled links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks).not.toBeNull();
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");
    });

    it("should sort links by order property if present", async () => {
      // Setup: All links are enabled
      const allEnabledLinks = [
        {
          url: "/test/1",
          title: "Fifth",
          enabled: true,
          order: 100,
        },
        { url: "/test/2", title: "Fourth", enabled: true, order: 50 },
        { url: "/test/3", title: "First", enabled: true, order: -100 },
        { url: "/test/3", title: "Second", enabled: true },
        { url: "/test/3", title: "Third", enabled: true },
      ];
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: allEnabledLinks },
      });

      // Verify all links are included in enabledLinks
      expect(wrapper.vm.enabledLinks.length).toBe(5);

      // Verify the order is correct based on the order property
      expect(wrapper.vm.enabledLinks[0].title).toBe("First");
      expect(wrapper.vm.enabledLinks[1].title).toBe("Second");
      expect(wrapper.vm.enabledLinks[2].title).toBe("Third");
      expect(wrapper.vm.enabledLinks[3].title).toBe("Fourth");
      expect(wrapper.vm.enabledLinks[4].title).toBe("Fifth");
    });

    it("should handle the case when all links are enabled", async () => {
      // Setup: All links are enabled
      const allEnabledLinks = [
        { url: "/system/info", title: "System Info", enabled: true },
        { url: "/system/log", title: "Logs", enabled: true },
        { url: "/system/config", title: "Configuration", enabled: true },
      ];
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: allEnabledLinks },
      });

      // Verify all links are included in enabledLinks
      expect(wrapper.vm.enabledLinks.length).toBe(3);
    });
    it("links without enabled property should be considered enabled", async () => {
      // Setup: All links are enabled
      const allEnabledLinks = [
        { url: "/system/info", title: "System Info" },
        { url: "/system/log", title: "Logs" },
        { url: "/system/config", title: "Configuration" },
      ];

      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: allEnabledLinks },
      });

      // Verify all links are included in enabledLinks
      expect(wrapper.vm.enabledLinks.length).toBe(3);
    });

    it("should handle the case when all links are disabled", async () => {
      // Setup: All links are disabled
      const allDisabledLinks = [
        { url: "/system/info", title: "System Info", enabled: false },
        { url: "/system/log", title: "Logs", enabled: false },
        { url: "/system/config", title: "Configuration", enabled: false },
      ];

      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: allDisabledLinks },
      });

      // Verify no links are included in enabledLinks
      expect(wrapper.vm.enabledLinks.length).toBe(0);
    });

    it("should handle an empty links array without header", async () => {
      // Setup: Empty links array

      // Mount the component
      const wrapper = await mountMainbarMenu({ props: { links: [] } });

      // Verify enabledLinks is also empty
      expect(wrapper.vm.enabledLinks.length).toBe(0);

      // Verify that only the header is rendered, no links
      expect(wrapper.findAll(".dropdown-header").length).toBe(0);
      expect(wrapper.findAll("a").length).toBe(0);
    });

    it("should handle an empty links array with header", async () => {
      // Setup: Empty links array

      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: [], header: "test" },
      });

      // Verify enabledLinks is also empty
      expect(wrapper.vm.enabledLinks.length).toBe(0);

      // Verify that only the header is rendered, no links
      expect(wrapper.findAll(".dropdown-header").length).toBe(1);
      expect(wrapper.findAll("a").length).toBe(0);
    });
  });

  // Test Case 3: Link and Submenu Rendering
  describe("Link and Submenu Rendering", () => {
    it("should correctly render simple links without header", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: simpleLinks },
      });

      // Verify that the correct number of links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks.length).toBe(2); // Only the enabled links should be rendered

      // Verify the content of the rendered links
      expect(renderedLinks.at(0)?.text()).toBe("System Info");
      expect(renderedLinks.at(1)?.text()).toBe("Logs");

      // Verify the href attributes
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");

      expect(wrapper.findAll(".dropdown-header").length).toBe(0);

      //verify icon
      expect(wrapper.findAll("i.fas.fa-cog.fa-lg").length).toBe(1);
    });
    it("should correctly render simple links with header", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: simpleLinks, header: "Something" },
      });

      // Verify that the correct number of links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks.length).toBe(2); // Only the enabled links should be rendered

      // Verify the content of the rendered links
      expect(renderedLinks.at(0)?.text()).toBe("System Info");
      expect(renderedLinks.at(1)?.text()).toBe("Logs");

      // Verify the href attributes
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");

      expect(wrapper.findAll(".dropdown-header").length).toBe(1);
      expect(wrapper.find(".dropdown-header").text()).toBe("Something");
    });
    it("should correctly render simple links with title", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: simpleLinks, title: "Menu Title" },
      });

      // Verify that the correct number of links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks.length).toBe(2); // Only the enabled links should be rendered

      // Verify the content of the rendered links
      expect(renderedLinks.at(0)?.text()).toBe("System Info");
      expect(renderedLinks.at(1)?.text()).toBe("Logs");

      // Verify the href attributes
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");

      expect(wrapper.findAll(".dropdown-header").length).toBe(0);
      expect(wrapper.findAll(".dropdown-toggle").length).toBe(1);
      expect(wrapper.find(".dropdown-toggle").text()).toContain("Menu Title");
    });
    it("should correctly render simple links with icon", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: {
          links: simpleLinks,
          header: "Something",
          iconCss: "fas fa-something fa-xs",
        },
      });

      // Verify that the correct number of links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks.length).toBe(2); // Only the enabled links should be rendered

      // Verify the content of the rendered links
      expect(renderedLinks.at(0)?.text()).toBe("System Info");
      expect(renderedLinks.at(1)?.text()).toBe("Logs");

      // Verify the href attributes
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");

      expect(wrapper.findAll(".dropdown-header").length).toBe(1);
      expect(wrapper.find(".dropdown-header").text()).toBe("Something");
      expect(wrapper.findAll("i.fas.fa-something.fa-xs").length).toBe(1);
    });

    it("should correctly render links with submenus", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: {
          links: [
            {
              title: "Plugins",
              enabled: true,
              links: [
                {
                  url: "/test/1",
                  title: "Test1",
                  enabled: true,
                },
                {
                  url: "/test/2",
                  title: "Test2",
                  enabled: true,
                },
              ],
            },
          ],
        },
      });
      // Verify that the correct number of links are rendered
      const renderedLinks = wrapper.findAll("a[href]");
      expect(renderedLinks.length).toBe(3); // Only the enabled links should be rendered

      // Verify the content of the rendered links
      expect(renderedLinks.at(0)?.text()).toBe("Plugins");
      expect(renderedLinks.at(0)?.attributes()["href"]).toBe("#");

      expect(renderedLinks.at(1)?.text()).toBe("Test1");
      expect(renderedLinks.at(2)?.text()).toBe("Test2");

      // Verify the href attributes
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/test/1");
      expect(renderedLinks.at(2)?.attributes("href")).toBe("/test/2");

      //submenu should be hidden before click
      const submenu = wrapper.find(".dropdown-submenu ul");
      expect(submenu.attributes()["style"]).toContain("display: none");
    });

    it("should correctly render links with separators", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: linksWithSeparators },
      });

      // Verify that separators are rendered
      const separators = wrapper.findAll('li[role="separator"]');
      expect(separators.length).toBe(2); // One for the separator, one for the group

      // Verify that the group separator has the correct ID
      const groupSeparator = wrapper.find('li[id="my-group"]');
      expect(groupSeparator.exists()).toBe(true);
    });
    it("should correctly render links which have urls and separators", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: linksWithEmbeddedSeparators },
      });

      // Verify that separators are rendered
      const separators = wrapper.findAll('li[role="separator"]');
      expect(separators.length).toBe(2); // One for the separator, one for the group

      // Verify that the group separator has the correct ID
      const groupSeparator = wrapper.find('li[id="my-group"]');
      expect(groupSeparator.exists()).toBe(true);
    });

    it("should correctly render links with icons", async () => {
      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: linksWithIcons },
      });

      // Verify that icons are rendered
      const icons = wrapper.findAll("i.glyphicon");
      expect(icons.length).toBe(2);

      // Verify that the icons have the correct classes
      expect(icons.at(0)?.classes()).toContain("glyphicon-info-sign");
      expect(icons.at(0)?.classes()).toContain("custom-class");
      expect(icons.at(1)?.classes()).toContain("glyphicon-list");
    });
  });

  // Test Case 4: Submenu Interaction
  describe("Submenu Interaction", () => {
    beforeEach(() => {});

    it("should update submenuOpen state when clicked", async () => {
      const wrapper = await mountMainbarMenu({
        props: { links: nestedLinks },
      });

      // Verify initially all submenus are closed
      expect(wrapper.vm.submenuOpen).toBe(null);

      //submenu should be hidden before click
      let submenu = wrapper.find(".dropdown-submenu ul");
      expect(submenu.attributes()["style"]).toContain("display: none");

      // Click the first submenu link by calling the method directly
      wrapper.vm.submenuClick(0, { stopPropagation: () => {} });

      // Verify the submenu is now tracked as open
      expect(wrapper.vm.submenuOpen).toBe(0); // First submenu index

      //await next tick
      await nextTick();
      //submenu should be shown after click
      submenu = wrapper.find(".dropdown-submenu ul");
      expect(submenu.attributes()["style"]).not.toContain("display: none");
      expect(submenu.attributes()["style"]).toContain("display: block");
    });

    it("should close an open submenu when clicked again", async () => {
      const wrapper = await mountMainbarMenu({
        props: { links: nestedLinks },
      });

      // Verify initially all submenus are closed
      expect(wrapper.vm.submenuOpen).toBe(null);

      // Click the first submenu link by calling the method directly
      wrapper.vm.submenuClick(0, { stopPropagation: () => {} });

      // Verify the submenu is now tracked as open
      expect(wrapper.vm.submenuOpen).toBe(0); // First submenu index

      //await next tick
      await nextTick();
      //submenu should be shown after click
      let submenu = wrapper.find(".dropdown-submenu ul");
      expect(submenu.attributes()["style"]).not.toContain("display: none");
      expect(submenu.attributes()["style"]).toContain("display: block");

      // Click the first submenu link again
      wrapper.vm.submenuClick(0, { stopPropagation: () => {} });

      // Verify the submenu is now tracked as open
      expect(wrapper.vm.submenuOpen).toBeNull(); // First submenu index

      //await next tick
      await nextTick();
      //submenu should be hidden again
      submenu = wrapper.find(".dropdown-submenu ul");
      expect(submenu.attributes()["style"]).not.toContain("display: block");
      expect(submenu.attributes()["style"]).toContain("display: none");
    });

    it("should close an open submenu when another submenu is clicked", async () => {
      const wrapper = await mountMainbarMenu({
        props: { links: nestedLinks },
      });

      const submenus = wrapper.findAll("li.dropdown-submenu ul.dropdown-menu");
      expect(submenus.length).toBe(2);

      // Verify initially all submenus are closed
      expect(wrapper.vm.submenuOpen).toBe(null);

      // Click the first submenu link by calling the method directly
      wrapper.vm.submenuClick(0, { stopPropagation: () => {} });

      // Verify the submenu is now tracked as open
      expect(wrapper.vm.submenuOpen).toBe(0); // First submenu index

      //await next tick
      await nextTick();

      //submenu 0 open
      expect(submenus[0].attributes()["style"]).not.toContain("display: none");
      expect(submenus[0].attributes()["style"]).toContain("display: block");

      //submenu 1 closed
      expect(submenus[1].attributes()["style"]).not.toContain("display: block");
      expect(submenus[1].attributes()["style"]).toContain("display: none");

      // Click the first submenu link again
      wrapper.vm.submenuClick(1, { stopPropagation: () => {} });

      // Verify the submenu is now tracked as open
      expect(wrapper.vm.submenuOpen).toBe(1); // First submenu index

      //await next tick
      await nextTick();

      //submenu 0 closed
      expect(submenus[0].attributes()["style"]).not.toContain("display: block");
      expect(submenus[0].attributes()["style"]).toContain("display: none");

      //submenu 1 open
      expect(submenus[1].attributes()["style"]).not.toContain("display: none");
      expect(submenus[1].attributes()["style"]).toContain("display: block");
    });

    it("should close a previously open submenu when clicking on a different one", async () => {
      const wrapper = await mountMainbarMenu({
        props: { links: nestedLinks },
      });

      // Click to open the first submenu
      wrapper.vm.submenuClick(0, { stopPropagation: () => {} });

      // Verify first submenu is open
      expect(wrapper.vm.submenuOpen).toBe(0);

      // Click the second submenu
      wrapper.vm.submenuClick(1, { stopPropagation: () => {} });

      // Verify first is closed and second is open
      expect(wrapper.vm.submenuOpen).toBe(1); // Index of the second submenu
    });

    it("should prevent event propagation when clicking on a submenu", async () => {
      const wrapper = await mountMainbarMenu({
        props: { links: nestedLinks },
      });

      // Create a mock event to track propagation
      const mockEvent = {
        stopPropagation: jest.fn(),
      };

      // Call the method directly with the mock event
      wrapper.vm.submenuClick(0, mockEvent);

      // Verify stopPropagation was called
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });
  });

  // Test Case 5: Edge Cases
  describe("Edge Cases", () => {
    it("should handle invalid links", async () => {
      // Setup: Links with missing properties
      const incompleteLinks = [
        { title: "No URL" }, // Missing url
        { url: "/system/info", title: "Info" }, // Has both properties
        { url: "/empty" }, // Missing title
        null, // null
        {}, // empty object
      ];

      // Mount the component
      const wrapper = await mountMainbarMenu({
        props: { links: incompleteLinks },
      });

      // Verify the component renders without errors
      expect(wrapper.vm.links).toEqual(incompleteLinks);

      const enabled = wrapper.vm.enabledLinks;
      // Verify the links are treated as enabled (no explicit enabled: false)
      expect(enabled).toEqual([
        { url: "/system/info", title: "Info" },
        { url: "/empty" },
      ]);
      expect(wrapper.vm.enabledLinks.length).toBe(2);
    });

    it("should handle deeply nested submenus", async () => {
      // Setup: Create deeply nested links structure
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
      const wrapper = await mountMainbarMenu({
        props: { links: deeplyNestedLinks },
      });

      // Verify the component renders the first level submenu
      const levelOneLink = wrapper.find(".dropdown-submenu > a");
      expect(levelOneLink.text()).toContain("Level 1");
    });
  });
});
