import { mount, VueWrapper } from "@vue/test-utils";
import { Btn, Dropdown } from "uiv";
import { nextTick } from "vue";

jest.mock("../../../../library", () => ({
  getRundeckContext: jest.fn(),
}));

import { getRundeckContext } from "../../../../library";
import AppUserMenu from "../user-menu/AppUserMenu.vue";
import MainbarMenu from "../../../../library/components/mainbar/MainbarMenu.vue";

const mockGetRundeckContext = getRundeckContext as jest.MockedFunction<
  typeof getRundeckContext
>;

const createWrapper = async (): Promise<VueWrapper<any>> => {
  const wrapper = mount(AppUserMenu, {
    global: {
      mocks: {
        $t: (key: string, args?: unknown[]) =>
          args && args.length ? `${key}:${args.join(",")}` : key,
      },
      provide: {
        rootStore: {},
      },
      components: { Dropdown, Btn, MainbarMenu },
    },
  });
  await nextTick();
  return wrapper;
};

describe("AppUserMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe("Context data available", () => {
    beforeEach(() => {
      mockGetRundeckContext.mockReturnValue({
        rdBase: "http://localhost:4440",
        profile: {
          username: "testuser",
          links: {
            profile: "http://localhost:4440/user/profile/custom",
            logout: "http://localhost:4440/user/logout/custom",
          },
        },
      } as any);
    });

    it("should display the username in the subheader", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="mainbar-menu-subheader"]').text(),
      ).toBe("userMenuGreeting:testuser");
    });

    it("should use the profile link from context", async () => {
      const wrapper = await createWrapper();

      const links = wrapper.findAll('[data-testid^="mainbar-menu-link-"]');
      expect(links.at(0)?.attributes("href")).toBe(
        "http://localhost:4440/user/profile/custom",
      );
    });

    it("should use the logout link from context", async () => {
      const wrapper = await createWrapper();

      const links = wrapper.findAll('[data-testid^="mainbar-menu-link-"]');
      expect(links.at(1)?.attributes("href")).toBe(
        "http://localhost:4440/user/logout/custom",
      );
    });
  });

  describe("Context data unavailable", () => {
    beforeEach(() => {
      mockGetRundeckContext.mockReturnValue({
        rdBase: "http://localhost:4440",
      } as any);
    });

    it("should display the unknown user text when username is unavailable", async () => {
      const wrapper = await createWrapper();

      expect(
        wrapper.find('[data-testid="mainbar-menu-subheader"]').text(),
      ).toBe("userMenuGreeting:unknownUser");
    });

    it("should fall back to default profile link when unavailable in context", async () => {
      const wrapper = await createWrapper();

      const links = wrapper.findAll('[data-testid^="mainbar-menu-link-"]');
      expect(links.at(0)?.attributes("href")).toBe(
        "http://localhost:4440/user/profile",
      );
    });

    it("should fall back to default logout link when unavailable in context", async () => {
      const wrapper = await createWrapper();

      const links = wrapper.findAll('[data-testid^="mainbar-menu-link-"]');
      expect(links.at(1)?.attributes("href")).toBe(
        "http://localhost:4440/user/logout",
      );
    });
  });

  describe("Rendering", () => {
    beforeEach(() => {
      mockGetRundeckContext.mockReturnValue({
        rdBase: "http://localhost:4440",
        profile: {
          username: "testuser",
        },
      } as any);
    });

    it("should render the user icon in the toggle", async () => {
      const wrapper = await createWrapper();

      const icon = wrapper.find('[data-testid="mainbar-menu-icon"]');
      expect(icon.classes()).toContain("fa-user");
    });

    it("should render translated profile and logout menu items", async () => {
      const wrapper = await createWrapper();

      const links = wrapper.findAll('[data-testid^="mainbar-menu-link-"]');
      expect(links.length).toBe(2);
      expect(links.at(0)?.text()).toBe("profile");
      expect(links.at(1)?.text()).toBe("logout");
    });
  });
});
