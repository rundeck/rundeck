import { mount, flushPromises } from "@vue/test-utils";
import Banner from "../Banner.vue";
import Message from "primevue/message";

const mockGetFeatureEnabled = jest.fn();
const mockLoadJsonData = jest.fn();
const mockNotificationNotify = jest.fn();
const mockEventBusEmit = jest.fn();
const mockCookies = {
  get: jest.fn(),
  set: jest.fn(),
  remove: jest.fn(),
};

jest.mock("@/library/services/feature", () => ({
  getFeatureEnabled: (featureName: string) => mockGetFeatureEnabled(featureName),
}));

jest.mock("@/app/utilities/loadJsonData", () => ({
  loadJsonData: (id: string) => mockLoadJsonData(id),
}));

jest.mock("@/library", () => ({
  getRundeckContext: jest.fn(() => ({
    eventBus: {
      on: jest.fn(),
      off: jest.fn(),
      emit: mockEventBusEmit,
    },
    rdBase: "http://localhost:4440/",
    apiVersion: "44",
  })),
}));

jest.mock("uiv", () => ({
  Notification: {
    notify: (options: any) => mockNotificationNotify(options),
  },
}));

interface MountOptions {
  props?: Record<string, any>;
  mocks?: Record<string, any>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(Banner, {
    props: {
      ...options.props,
    },
    global: {
      components: { Message },
      mocks: {
        $cookies: mockCookies,
        ...options.mocks,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("Banner", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetFeatureEnabled.mockResolvedValue(false);
    mockLoadJsonData.mockReturnValue({ isAdmin: true });
    mockCookies.get.mockReturnValue(undefined);
  });

  describe("Banner visibility", () => {
    it("shows banner when user is admin and feature is disabled and no dismissal cookies are set", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: true });
      mockGetFeatureEnabled.mockResolvedValue(false);
      mockCookies.get.mockReturnValue(undefined);

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(true);
    });

    it("hides banner when user is not admin", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: false });
      mockGetFeatureEnabled.mockResolvedValue(false);
      mockCookies.get.mockReturnValue(undefined);

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("hides banner when feature is already enabled", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: true });
      mockGetFeatureEnabled.mockResolvedValue(true);
      mockCookies.get.mockReturnValue(undefined);

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("hides banner when permanent dismissal cookie is set", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: true });
      mockGetFeatureEnabled.mockResolvedValue(false);
      mockCookies.get.mockImplementation((name: string) => {
        if (name === "conditionalEarlyAccessBannerPermanentlyDismissed") {
          return "true";
        }
        return undefined;
      });

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("shows banner again when only reminded cookie is set", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: true });
      mockGetFeatureEnabled.mockResolvedValue(false);
      mockCookies.get.mockImplementation((name: string) => {
        if (name === "conditionalEarlyAccessBannerReminded") {
          return "true";
        }
        return undefined;
      });

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(true);
    });
  });

  describe("Banner content", () => {
    describe("when banner is visible", () => {
      let wrapper: any;

      beforeEach(async () => {
        wrapper = await createWrapper();
      });

      it("displays early access title", () => {
        expect(wrapper.find('[data-testid="banner-title"]').text()).toBe("earlyAccess");
      });

      it("displays early access description", () => {
        expect(wrapper.find('[data-testid="banner-description"]').text()).toBe("earlyAccessDescriptionWorkflow");
      });

      it("displays Try Now button", () => {
        expect(wrapper.find('[data-testid="banner-try-now-button"]').text()).toBe("earlyAccessTryNow");
      });

      it("displays Remind Me Later button", () => {
        expect(wrapper.find('[data-testid="banner-remind-later-button"]').exists()).toBe(true);
      });
    });

    describe("when user has been reminded", () => {
      let wrapper: any;

      beforeEach(async () => {
        mockCookies.get.mockImplementation((name: string) => {
          if (name === "conditionalEarlyAccessBannerReminded") {
            return "true";
          }
          return undefined;
        });
        wrapper = await createWrapper();
      });

      it("hides Remind Me Later button", () => {
        expect(wrapper.find('[data-testid="banner-remind-later-button"]').exists()).toBe(false);
      });
    });
  });

  describe("Try Now button", () => {
    let wrapper: any;

    beforeEach(async () => {
      wrapper = await createWrapper();
    });

    it("emits conditional-early-access:try-now event when clicked", async () => {
      await wrapper.find('[data-testid="banner-try-now-button"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(mockEventBusEmit).toHaveBeenCalledWith("conditional-early-access:try-now");
    });
  });

  describe("Remind Me Later button", () => {
    let wrapper: any;

    beforeEach(async () => {
      wrapper = await createWrapper();
      await wrapper.find('[data-testid="banner-remind-later-button"]').trigger("click");
      await wrapper.vm.$nextTick();
    });

    it("sets reminded cookie when clicked", () => {
      expect(mockCookies.set).toHaveBeenCalledWith(
        "conditionalEarlyAccessBannerReminded",
        "true",
        "1y",
        "/",
        "",
        false,
        "Strict"
      );
    });

    it("shows toast notification when clicked", () => {
      expect(mockNotificationNotify).toHaveBeenCalledWith({
        type: "info",
        title: "",
        content: "earlyAccessRemindLaterToast",
      });
    });

    it("hides banner immediately when clicked", () => {
      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });
  });

  describe("Close button", () => {
    let wrapper: any;

    beforeEach(async () => {
      mockCookies.get.mockImplementation((name: string) => {
        if (name === "conditionalEarlyAccessBannerReminded") {
          return "true";
        }
        return undefined;
      });

      wrapper = await createWrapper();
      const messageComponent = wrapper.findComponent(Message);
      await messageComponent.vm.$emit("close");
      await wrapper.vm.$nextTick();
    });

    it("sets permanent dismissal cookie when close button is clicked", () => {
      expect(mockCookies.set).toHaveBeenCalledWith(
        "conditionalEarlyAccessBannerPermanentlyDismissed",
        "true",
        "1y",
        "/",
        "",
        false,
        "Strict"
      );
    });

    it("removes reminded cookie when close button is clicked", () => {
      expect(mockCookies.remove).toHaveBeenCalledWith(
        "conditionalEarlyAccessBannerReminded",
        "/"
      );
    });

    it("shows toast notification when close button is clicked", () => {
      expect(mockNotificationNotify).toHaveBeenCalledWith({
        type: "info",
        title: "",
        content: "earlyAccessGoToSettings",
      });
    });

    it("hides banner permanently when close button is clicked", () => {
      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });
  });

  describe("Edge cases", () => {
    it("handles null jobEditUiMeta", async () => {
      mockLoadJsonData.mockReturnValue(null);

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("handles undefined jobEditUiMeta", async () => {
      mockLoadJsonData.mockReturnValue(undefined);

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("handles isAdmin as string value true", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: "true" });

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(true);
    });

    it("handles isAdmin as string value false", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: "false" });

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("handles both cookies being set", async () => {
      mockLoadJsonData.mockReturnValue({ isAdmin: true });
      mockGetFeatureEnabled.mockResolvedValue(false);
      mockCookies.get.mockImplementation((name: string) => {
        if (name === "conditionalEarlyAccessBannerReminded") {
          return "true";
        }
        if (name === "conditionalEarlyAccessBannerPermanentlyDismissed") {
          return "true";
        }
        return undefined;
      });

      const wrapper = await createWrapper();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(false);
    });

    it("handles feature check failure gracefully", async () => {
      mockGetFeatureEnabled.mockRejectedValue(new Error("API Error"));

      const wrapper = await createWrapper();
      await flushPromises();

      expect(wrapper.find('[data-testid="banner-message"]').exists()).toBe(true);
    });
  });
});
