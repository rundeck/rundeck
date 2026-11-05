import { mount, VueWrapper } from "@vue/test-utils";
import UserTable from "../UserTable.vue";
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
  { login: "bob", firstName: "Bob", lastName: "Brown", email: "b@x.com" },
];

const mountUserTable = async (
  props: Record<string, any>,
): Promise<VueWrapper<any>> => {
  const wrapper = mount(UserTable, {
    props: { appAdmin: false, currentUser: "", ...props },
    global: {
      mocks: { $t },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("UserTable", () => {
  it("renders one row per user", async () => {
    const wrapper = await mountUserTable({ users });

    const rows = wrapper.findAllComponents({ name: "UserListRow" });
    expect(rows.length).toBe(2);
  });

  it("renders an empty table when there are no users", async () => {
    const wrapper = await mountUserTable({ users: [] });

    const rows = wrapper.findAllComponents({ name: "UserListRow" });
    expect(rows.length).toBe(0);
  });

  it("passes appAdmin and currentUser through to each row", async () => {
    const wrapper = await mountUserTable({
      users,
      appAdmin: true,
      currentUser: "alice",
    });

    const rows = wrapper.findAllComponents({ name: "UserListRow" });
    expect(rows[0].props("appAdmin")).toBe(true);
    expect(rows[0].props("currentUser")).toBe("alice");
  });
});
