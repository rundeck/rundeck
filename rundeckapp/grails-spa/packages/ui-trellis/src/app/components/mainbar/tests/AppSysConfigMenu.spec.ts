import { shallowMount, mount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
import { nextTick } from "vue";

// Mock the loadJsonData function
jest.mock("../../../utilities/loadJsonData", () => ({
  loadJsonData: jest.fn(),
}));

// Import the mocked function
import { loadJsonData } from "../../../utilities/loadJsonData";
import AppSysConfigMenu from "../sysconfig/AppSysConfigMenu.vue";

const mockLoadJsonData = loadJsonData as jest.MockedFunction<
  typeof loadJsonData
>;

// Test fixtures
const simpleLinks = [
  { url: "/system/info", title: "System Info", enabled: true },
  { url: "/system/log", title: "Logs" },
  { url: "/system/config", title: "Configuration", enabled: false },
];

describe("AppSysConfigMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const mountSysConfigMenu = async (
    options?: Record<string, any>,
  ): Promise<VueWrapper<any>> => {
    const wrapper = shallowMount(AppSysConfigMenu, {
      ...options,
      global: {
        components: {
          Dropdown,
          Btn,
        },
        stubs: {
          SysConfigMenu: {
            template: `<div data-test="sys-config-menu-stub" asdf></div>`,
          },
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
      // Setup: Mock loadJsonData to return a mix of enabled and disabled links
      mockLoadJsonData.mockReturnValue(simpleLinks);
      // Mount the component
      const wrapper = await mountSysConfigMenu();

      // Verify that loadJsonData was called with the correct ID
      expect(mockLoadJsonData).toHaveBeenCalledWith("sysConfigNavJSON");

      // Verify the data was loaded correctly
      expect(wrapper.vm.links).toEqual(simpleLinks);
    });

    it("should handle case when DOM element does not exist or contains invalid data", async () => {
      // Setup: Mock loadJsonData to return a mix of enabled and disabled links
      mockLoadJsonData.mockReturnValue([]);
      // Mount the component
      const wrapper = await mountSysConfigMenu();

      // Verify that loadJsonData was called
      expect(mockLoadJsonData).toHaveBeenCalled();

      // Verify the links array is empty
      expect(wrapper.vm.links).toEqual([]);
    });
    it("should handle null data from loadJsonData", async () => {
      // Setup: Mock loadJsonData to return a mix of enabled and disabled links
      mockLoadJsonData.mockReturnValue(null);
      // Mount the component
      const wrapper = await mountSysConfigMenu();

      // Verify that loadJsonData was called
      expect(mockLoadJsonData).toHaveBeenCalled();

      // Verify the links array is empty
      expect(wrapper.vm.links).toEqual([]);
    });
  });
});
