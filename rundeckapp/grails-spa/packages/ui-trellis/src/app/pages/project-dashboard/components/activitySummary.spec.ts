//@ts-nocheck
import { mount } from "@vue/test-utils";
import ActivitySummary from "./activitySummary.vue";

const mountActivitySummary = async (props = {}) => {
  return mount(ActivitySummary, {
    props: {
      project: {},
      rdBase: "",
      ...props,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
        $tc: (msg, count) => {
          return `${msg}/${count}`;
        },
      },
      stubs: {
        "i18n-t": {
          props: ["keypath"],
          template:
            'i18n-t: {{keypath}} <slot name="count"></slot> <slot name="users"></slot>',
        },
      },
    },
  });
};

describe("EditProjectFile", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("links to project activity", async () => {
    const wrapper = await mountActivitySummary({
      project: { name: "test" },
      rdBase: "http://localhost:9999/",
    });
    expect(wrapper.find(".card-content a.h4").attributes()["href"]).toBe(
      "http://localhost:9999/project/test/activity",
    );
  });

  it.each([0, 1, 2])("renders summary count %d", async (execCount) => {
    const wrapper = await mountActivitySummary({
      project: { execCount, name: "test", userCount: 0 },
    });
    expect(wrapper.find(".card-content .summary-count").text()).toContain(
      execCount.toString(),
    );
    expect(wrapper.find(".card-content a.h4").text()).toContain(
      execCount.toString() + " execution/" + execCount.toString(),
    );
  });
  it.each([1, 2])("renders failed count %d", async (failedCount) => {
    const wrapper = await mountActivitySummary({
      project: { failedCount, name: "test" },
      rdBase: "http://localhost:9999/",
    });
    expect(
      wrapper.find(`.card-content [data-test-id="failed-count"]`).text(),
    ).toContain(`project.activitySummary.failedCount`);
    expect(
      wrapper
        .find(`.card-content [data-test-id="failed-count"] a`)
        .attributes()["href"],
    ).toBe("http://localhost:9999/project/test/activity?statFilter=fail");
  });
  it("does not failed count 0", async () => {
    const wrapper = await mountActivitySummary({
      project: { failedCount: 0, name: "test" },
      rdBase: "http://localhost:9999/",
    });
    expect(
      wrapper.findAll(`.card-content [data-test-id="failed-count"]`).length,
    ).toBe(0);
  });
  it.each([1, 2])("renders user count %d", async (userCount) => {
    const wrapper = await mountActivitySummary({
      project: { userCount, name: "test" },
      rdBase: "http://localhost:9999/",
    });
    expect(
      wrapper.find(`.card-content [data-test-id="user-count"]`).text(),
    ).toContain(
      `i18n-t: project.activitySummary.userCount ${userCount} users.plural/${userCount}`,
    );
  });
  it.each([["a"], ["a", "b"], ["a", "b", "c"]])(
    "renders user list",
    async (userSummary) => {
      const wrapper = await mountActivitySummary({
        project: { userSummary, userCount: userSummary.length, name: "test" },
        rdBase: "http://localhost:9999/",
      });
      expect(
        wrapper.findAll(`.card-content [data-test-id="user-count"] ul.users li`)
          .length,
      ).toBe(userSummary.length);
    },
  );
  it("does not show user count 0", async () => {
    const wrapper = await mountActivitySummary({
      project: { userCount: 0, name: "test" },
      rdBase: "http://localhost:9999/",
    });
    expect(
      wrapper.findAll(`.card-content [data-test-id="user-count"]`).length,
    ).toBe(0);
  });
});
