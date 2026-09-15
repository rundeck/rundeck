import { mount, VueWrapper } from "@vue/test-utils";
import UserListPage from "../UserListPage.vue";
import messages from "../../../utilities/locales/en_US.js";

const $t = (key: string) => (messages as Record<string, string>)[key] || key;

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440" }),
}));

const users = [
  {
    login: "alice",
    firstName: "Alice",
    lastName: "Anderson",
    email: "a@x.com",
  },
];

const mountUserListPage = async (
  props: Record<string, any>,
): Promise<VueWrapper<any>> => {
  const wrapper = mount(UserListPage, {
    props: { users, appAdmin: false, currentUser: "", ...props },
    global: {
      mocks: { $t },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("UserListPage", () => {
  it("renders the Users heading and a table of users", async () => {
    const wrapper = await mountUserListPage({});

    expect(wrapper.find("h3").text()).toContain("Users");
    expect(wrapper.findComponent({ name: "UserTable" }).exists()).toBe(true);
  });

  it("does not show the New Profile link for non app-admins", async () => {
    const wrapper = await mountUserListPage({ appAdmin: false });

    expect(wrapper.text()).not.toContain("New Profile");
  });

  it("shows a New Profile link pointing to /user/create for app-admins", async () => {
    const wrapper = await mountUserListPage({ appAdmin: true });

    const link = wrapper.find("a.btn");
    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toBe("http://localhost:4440/user/create");
    expect(wrapper.text()).toContain("New Profile");
  });

  it("passes the users, appAdmin, and currentUser down to the table", async () => {
    const wrapper = await mountUserListPage({
      users,
      appAdmin: true,
      currentUser: "alice",
    });

    const table = wrapper.findComponent({ name: "UserTable" });
    expect(table.props("users")).toEqual(users);
    expect(table.props("appAdmin")).toBe(true);
    expect(table.props("currentUser")).toBe("alice");
  });
});
