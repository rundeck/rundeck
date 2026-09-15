import { mount, VueWrapper } from "@vue/test-utils";
import UserDetailPanel from "../UserDetailPanel.vue";
import messages from "../../../utilities/locales/en_US.js";

const $t = (key: string) => (messages as Record<string, string>)[key] || key;

const mountUserDetailPanel = async (
  props: Record<string, any>,
): Promise<VueWrapper<any>> => {
  const wrapper = mount(UserDetailPanel, {
    props,
    global: {
      mocks: { $t },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("UserDetailPanel", () => {
  it("shows email, first name, and last name when populated", async () => {
    const wrapper = await mountUserDetailPanel({
      user: {
        login: "alice",
        email: "alice@example.com",
        firstName: "Alice",
        lastName: "Anderson",
      },
    });

    expect(wrapper.text()).toContain("alice@example.com");
    expect(wrapper.text()).toContain("Alice");
    expect(wrapper.text()).toContain("Anderson");
    expect(wrapper.text()).not.toContain("Not set");
  });

  it("shows NOT SET placeholders for blank fields", async () => {
    const wrapper = await mountUserDetailPanel({
      user: { login: "bob" },
    });

    const notSetCount = wrapper.text().split("Not set").length - 1;
    expect(notSetCount).toBe(3);
  });

  it("does not render the groups column when showGroups is false", async () => {
    const wrapper = await mountUserDetailPanel({
      user: { login: "bob" },
      showGroups: false,
    });

    expect(wrapper.text()).not.toContain("Groups");
  });

  it("renders the groups column when showGroups is true", async () => {
    const wrapper = await mountUserDetailPanel({
      user: { login: "alice" },
      showGroups: true,
      groups: "admin, user",
    });

    expect(wrapper.text()).toContain("Groups");
    expect(wrapper.text()).toContain("admin, user");
  });
});
