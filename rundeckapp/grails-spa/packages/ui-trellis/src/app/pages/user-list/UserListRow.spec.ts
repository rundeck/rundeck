import { mount } from "@vue/test-utils";
import UserListRow from "./UserListRow.vue";

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440/" }),
}));

const createWrapper = (props = {}) => {
  return mount(UserListRow, {
    props: {
      user: {
        login: "alice",
        firstName: "Alice",
        lastName: "Anderson",
        email: "alice@example.com",
      },
      editAuthAllowed: true,
      index: 0,
      ...props,
    },
  });
};

describe("UserListRow", () => {
  it("displays the user login", () => {
    const wrapper = createWrapper();
    expect(wrapper.find(".userlogin").text()).toBe("alice");
  });

  it("displays the user's full name", () => {
    const wrapper = createWrapper();
    expect(wrapper.find(".username").text()).toBe("Alice Anderson");
  });

  it("displays the email wrapped in angle brackets", () => {
    const wrapper = createWrapper();
    expect(wrapper.find(".useremail").text()).toBe("<alice@example.com>");
  });

  it("does not render an email element when the user has no email", () => {
    const wrapper = createWrapper({
      user: { login: "bob", firstName: "Bob", lastName: "Brown" },
    });
    expect(wrapper.find(".useremail").exists()).toBe(false);
  });

  it("renders an edit link to the user edit action when editing is allowed", () => {
    const wrapper = createWrapper();
    const link = wrapper.find(".useredit a");
    expect(link.exists()).toBe(true);
    expect(link.attributes("href")).toBe(
      "http://localhost:4440/user/edit?login=alice",
    );
  });

  it("url-encodes the login in the edit link", () => {
    const wrapper = createWrapper({
      user: { login: "a b/c", firstName: "A", lastName: "B" },
    });
    expect(wrapper.find(".useredit a").attributes("href")).toBe(
      "http://localhost:4440/user/edit?login=a%20b%2Fc",
    );
  });

  it("hides the edit link when editing is not allowed", () => {
    const wrapper = createWrapper({ editAuthAllowed: false });
    expect(wrapper.find(".useredit").exists()).toBe(false);
  });

  it("applies the alternateRow class to odd-indexed rows", () => {
    const wrapper = createWrapper({ index: 1 });
    expect(wrapper.find("tr").classes()).toContain("alternateRow");
  });

  it("does not apply the alternateRow class to even-indexed rows", () => {
    const wrapper = createWrapper({ index: 2 });
    expect(wrapper.find("tr").classes()).not.toContain("alternateRow");
  });

  it("does not render the user's email as HTML markup", () => {
    const wrapper = createWrapper({
      user: {
        login: "mallory",
        firstName: "Mal",
        lastName: "Lory",
        email: "<img src=x onerror=alert(1)>",
      },
    });
    // The angle brackets should be displayed as literal text, not parsed as an element.
    expect(wrapper.find(".useremail").find("img").exists()).toBe(false);
    expect(wrapper.find(".useremail").text()).toContain("onerror=alert(1)");
  });
});
