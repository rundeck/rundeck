import { mount, VueWrapper } from "@vue/test-utils";
import UserListRow from "../UserListRow.vue";
import messages from "../../../utilities/locales/en_US.js";

const $t = (key: string) => (messages as Record<string, string>)[key] || key;

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440" }),
}));

const mountUserListRow = async (
  props: Record<string, any>,
): Promise<VueWrapper<any>> => {
  const wrapper = mount(UserListRow, {
    props: {
      index: 0,
      appAdmin: false,
      currentUser: "",
      ...props,
    },
    global: {
      mocks: { $t },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("UserListRow", () => {
  const user = {
    login: "alice",
    firstName: "Alice",
    lastName: "Anderson",
    email: "alice@example.com",
  };

  it("shows the user's login, name, and email", async () => {
    const wrapper = await mountUserListRow({ user });

    expect(wrapper.find(".userlogin").text()).toBe("alice");
    expect(wrapper.text()).toContain("Alice Anderson");
    expect(wrapper.text()).toContain("alice@example.com");
  });

  it("does not show an edit link when the viewer is not an app admin", async () => {
    const wrapper = await mountUserListRow({ user, appAdmin: false });

    expect(wrapper.find('[data-testid="user-edit-alice"]').exists()).toBe(
      false,
    );
  });

  it("shows an edit link pointing to /user/edit when the viewer is an app admin", async () => {
    const wrapper = await mountUserListRow({ user, appAdmin: true });

    const editLink = wrapper.find('[data-testid="user-edit-alice"]');
    expect(editLink.exists()).toBe(true);
    expect(editLink.attributes("href")).toBe(
      "http://localhost:4440/user/edit?login=alice",
    );
  });

  it("toggles the detail panel open and closed when the expander is clicked", async () => {
    const wrapper = await mountUserListRow({ user });

    expect(wrapper.findComponent({ name: "UserDetailPanel" }).exists()).toBe(
      false,
    );

    await wrapper.find('[data-testid="user-expander-alice"]').trigger("click");
    expect(wrapper.findComponent({ name: "UserDetailPanel" }).exists()).toBe(
      true,
    );

    await wrapper.find('[data-testid="user-expander-alice"]').trigger("click");
    expect(wrapper.findComponent({ name: "UserDetailPanel" }).exists()).toBe(
      false,
    );
  });

  it("shows the groups column only when viewing your own row", async () => {
    const wrapper = await mountUserListRow({
      user,
      currentUser: "alice",
    });
    await wrapper.find('[data-testid="user-expander-alice"]').trigger("click");
    expect(
      wrapper.findComponent({ name: "UserDetailPanel" }).props("showGroups"),
    ).toBe(true);

    const otherWrapper = await mountUserListRow({
      user,
      currentUser: "someoneElse",
    });
    await otherWrapper
      .find('[data-testid="user-expander-alice"]')
      .trigger("click");
    expect(
      otherWrapper
        .findComponent({ name: "UserDetailPanel" })
        .props("showGroups"),
    ).toBe(false);
  });
});
