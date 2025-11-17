import { mount, shallowMount, VueWrapper } from "@vue/test-utils";
// Import the mocked function for use in tests
import { getRundeckContext } from "../../../../library";
import { Dropdown, Btn } from "uiv";
import UserMenu from "../user-menu/UserMenu.vue";

describe("UserMenu", () => {
  const mountAppUserMenu = async (props: any): Promise<VueWrapper<any>> => {
    return mount(UserMenu, {
      props,
      global: {
        mocks: {
          $t: (key: string) => key, // Simple mock for translation function
        },
        components: {
          Dropdown,
          Btn,
        },
      },
    });
  };

  describe("Test Case 1: Context data available", () => {
    it("should display the correct username", async () => {
      const wrapper = await mountAppUserMenu({
        username: "testuser",
        profileLink: "http://localhost:4440/user/profile/custom",
        logoutLink: "http://localhost:4440/user/logout/custom",
      });

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      expect(wrapper.find("div").text()).toContain("Hi testuser!");
    });

    it("should use the profile link prop", async () => {
      const wrapper = await mountAppUserMenu({
        username: "testuser",
        profileLink: "http://localhost:4440/user/profile/custom",
        logoutLink: "http://localhost:4440/user/logout/custom",
      });

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const profileLink = wrapper.find(
        'a[href="http://localhost:4440/user/profile/custom"]',
      );
      expect(profileLink.exists()).toBe(true);
    });

    it("should use the logout link prop", async () => {
      const wrapper = await mountAppUserMenu({
        username: "testuser",
        profileLink: "http://localhost:4440/user/profile/custom",
        logoutLink: "http://localhost:4440/user/logout/custom",
      });

      // Wait for the component to be mounted and data to be populated
      await wrapper.vm.$nextTick();

      const logoutLink = wrapper.find(
        'a[href="http://localhost:4440/user/logout/custom"]',
      );
      expect(logoutLink.exists()).toBe(true);
    });
  });

  // Additional comprehensive tests
  it("should render the dropdown component", async () => {
    const wrapper = await mountAppUserMenu({
      username: "testuser",
      profileLink: "http://localhost:4440/user/profile/custom",
      logoutLink: "http://localhost:4440/user/logout/custom",
    });

    expect(wrapper.findComponent({ name: "Dropdown" }).exists()).toBe(true);
  });

  it("should render the user icon", async () => {
    const wrapper = await mountAppUserMenu({
      username: "testuser",
      profileLink: "http://localhost:4440/user/profile/custom",
      logoutLink: "http://localhost:4440/user/logout/custom",
    });

    expect(wrapper.find("i.fa-user").exists()).toBe(true);
  });

  it("should have the correct translations for menu items", async () => {
    const wrapper = await mountAppUserMenu({
      username: "testuser",
      profileLink: "http://localhost:4440/user/profile/custom",
      logoutLink: "http://localhost:4440/user/logout/custom",
    });
    await wrapper.vm.$nextTick();

    const profileMenuItem = wrapper.findAll("a").at(0);
    const logoutMenuItem = wrapper.findAll("a").at(1);

    expect(profileMenuItem?.text()).toBe("profile");
    expect(logoutMenuItem?.text()).toBe("logout");
  });
});
