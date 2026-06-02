import { mount } from "@vue/test-utils";
import UserListPage from "./UserListPage.vue";

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440/" }),
}));

const sampleUsers = [
  {
    login: "alice",
    firstName: "Alice",
    lastName: "Anderson",
    email: "alice@example.com",
  },
  { login: "bob", firstName: "Bob", lastName: "Brown" },
];

const createWrapper = (props = {}) => {
  return mount(UserListPage, {
    props: {
      users: sampleUsers,
      createAuthAllowed: true,
      editAuthAllowed: true,
      ...props,
    },
  });
};

describe("UserListPage", () => {
  it("renders the heading", () => {
    const wrapper = createWrapper();
    expect(wrapper.find("h3").text()).toContain("user.list.heading");
  });

  it("renders one row per user", () => {
    const wrapper = createWrapper();
    expect(wrapper.findAll("tbody tr")).toHaveLength(2);
  });

  it("renders each user's login", () => {
    const wrapper = createWrapper();
    const logins = wrapper.findAll(".userlogin").map((n) => n.text());
    expect(logins).toEqual(["alice", "bob"]);
  });

  it("renders a create link to the user create action when creation is allowed", () => {
    const wrapper = createWrapper();
    const link = wrapper.find("h3 a");
    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toBe("http://localhost:4440/user/create");
    expect(link.text()).toContain("user.list.newProfile");
  });

  it("hides the create link when creation is not allowed", () => {
    const wrapper = createWrapper({ createAuthAllowed: false });
    expect(wrapper.find("h3 a").exists()).toBe(false);
  });

  it("renders edit links on rows when editing is allowed", () => {
    const wrapper = createWrapper();
    expect(wrapper.findAll(".useredit a").length).toBe(2);
  });

  it("hides all edit links when editing is not allowed", () => {
    const wrapper = createWrapper({ editAuthAllowed: false });
    expect(wrapper.find(".useredit").exists()).toBe(false);
  });

  it("renders an empty table when there are no users", () => {
    const wrapper = createWrapper({ users: [] });
    expect(wrapper.findAll("tbody tr")).toHaveLength(0);
    expect(wrapper.find("table.userlist").exists()).toBe(true);
  });
});
