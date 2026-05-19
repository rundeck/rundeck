import { mount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
import { nextTick } from "vue";

jest.mock("../../../utilities/loadJsonData", () => ({
  loadJsonData: jest.fn(),
}));

import { loadJsonData } from "../../../utilities/loadJsonData";
import AppSysConfigMenu from "../sysconfig/AppSysConfigMenu.vue";
import MainbarMenu from "../../../../library/components/mainbar/MainbarMenu.vue";

const mockLoadJsonData = loadJsonData as jest.MockedFunction<
  typeof loadJsonData
>;

const simpleLinks = [
  { url: "/system/info", title: "System Info", enabled: true },
  { url: "/system/log", title: "Logs" },
  { url: "/system/config", title: "Configuration", enabled: false },
];

const createWrapper = async (
  options: Record<string, any> = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(AppSysConfigMenu, {
    ...options,
    global: {
      mocks: {
        $t: (key: string) => key,
      },
      components: { Dropdown, Btn, MainbarMenu },
    },
  });
  await nextTick();
  return wrapper;
};

describe("AppSysConfigMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("Data Loading", () => {
    it("should load valid JSON data and pass it to MainbarMenu", async () => {
      mockLoadJsonData.mockReturnValue(simpleLinks);

      const wrapper = await createWrapper();

      expect(mockLoadJsonData).toHaveBeenCalledWith("sysConfigNavJSON");

      const inner = wrapper.findComponent(MainbarMenu);
      expect(inner.props("links")).toEqual(simpleLinks);

      const renderedLinks = wrapper.findAll(
        '[data-testid^="mainbar-menu-link-"]',
      );
      expect(renderedLinks.length).toBe(2);
      expect(renderedLinks.at(0)?.attributes("href")).toBe("/system/info");
      expect(renderedLinks.at(1)?.attributes("href")).toBe("/system/log");
    });

    it("should not render MainbarMenu when loadJsonData returns an empty array", async () => {
      mockLoadJsonData.mockReturnValue([]);

      const wrapper = await createWrapper();

      expect(mockLoadJsonData).toHaveBeenCalledWith("sysConfigNavJSON");
      expect(wrapper.findComponent(MainbarMenu).exists()).toBe(false);
    });

    it("should not render MainbarMenu when loadJsonData returns null", async () => {
      mockLoadJsonData.mockReturnValue(null);

      const wrapper = await createWrapper();

      expect(mockLoadJsonData).toHaveBeenCalledWith("sysConfigNavJSON");
      expect(wrapper.findComponent(MainbarMenu).exists()).toBe(false);
    });

    it("should render the System header when data is loaded", async () => {
      mockLoadJsonData.mockReturnValue(simpleLinks);

      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="mainbar-menu-header"]').text(),
      ).toBe("sysConfigMenuHeader");
    });
  });
});
