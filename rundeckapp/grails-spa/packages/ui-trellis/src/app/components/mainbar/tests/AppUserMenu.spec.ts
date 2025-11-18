import { mount, shallowMount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
// Import the mocked function for use in tests
import { getRundeckContext } from "../../../../library";
import AppUserMenu from "../user-menu/AppUserMenu.vue";
import MainbarMenu from "../../../../library/components/mainbar/MainbarMenu.vue";
// Mock the getRundeckContext function
jest.mock("../../../../library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    apiVersion: "44",
  })),
}));

const mockGetRundeckContext = getRundeckContext as jest.MockedFunction<
  typeof getRundeckContext
>;

describe("AppUserMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const mountAppUserMenu = async (): Promise<VueWrapper<any>> => {
    return mount(AppUserMenu, {
      global: {
        mocks: {
          $t: (key: string) => key, // Simple mock for translation function
        },
        components: {
          Dropdown,
          Btn,
          MainbarMenu,
        },
      },
    });
  };

  describe("Test Case 1: Context data available", () => {
    beforeEach(() => {
      // Mock the context with profile data
    });

    it("should pass the correct username", async () => {
      (
        getRundeckContext as jest.MockedFunction<typeof getRundeckContext>
      ).mockReturnValue({
        rdBase: "http://localhost:4440/",
        profile: {
          username: "testuser",
          links: {
            profile: "http://localhost:4440/user/profile/custom",
            logout: "http://localhost:4440/user/logout/custom",
          },
        },
      } as any);
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      expect(wrapper.find("div").text()).toContain("testuser");
    });

    it("should use the profile link from context", async () => {
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const profileLink = wrapper.find(
        'a[href="http://localhost:4440/user/profile/custom"]',
      );
      expect(profileLink.exists()).toBe(true);
    });

    it("should use the logout link from context", async () => {
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const logoutLink = wrapper.find(
        'a[href="http://localhost:4440/user/logout/custom"]',
      );
      expect(logoutLink.exists()).toBe(true);
    });
  });

  describe("Test Case 2: Context data unavailable", () => {
    beforeEach(() => {
      // Mock the context without profile data
      mockGetRundeckContext.mockReturnValue({
        rdBase: "http://localhost:4440/",
        // No profile data
      } as any);
    });

    it("should display the unknown user text when username is unavailable", async () => {
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      expect(wrapper.find("div").text()).toContain("Hi (Unknown User)!");
    });

    it("should fallback to default profile link when unavailable in context", async () => {
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const profileLink = wrapper.find(
        'a[href="http://localhost:4440/user/profile"]',
      );
      expect(profileLink.exists()).toBe(true);
    });

    it("should fallback to default logout link when unavailable in context", async () => {
      const wrapper = await mountAppUserMenu();

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const logoutLink = wrapper.find(
        'a[href="http://localhost:4440/user/logout"]',
      );
      expect(logoutLink.exists()).toBe(true);
    });
  });

  // Additional comprehensive tests
  it("should render the MainbarMenu component", async () => {
    const wrapper = await mountAppUserMenu();

    expect(wrapper.find(".dropdown-menu").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "MainbarMenu" }).exists()).toBe(true);
  });

  it("should render the user icon", async () => {
    const wrapper = await mountAppUserMenu();

    expect(wrapper.find("i.fa-user").exists()).toBe(true);
  });

  it("should have the correct translations for menu items", async () => {
    const wrapper = await mountAppUserMenu();
    await wrapper.vm.$nextTick();

    const profileMenuItem = wrapper.findAll("a").at(0);
    const logoutMenuItem = wrapper.findAll("a").at(1);

    expect(profileMenuItem?.text()).toBe("profile");
    expect(logoutMenuItem?.text()).toBe("logout");
  });
});
