import { mount, VueWrapper } from "@vue/test-utils";
import JobScmStatus from "../tree/JobScmStatus.vue";

jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));
const mountJobScmStatus = async (props: any): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobScmStatus, {
    props,
    global: {
      mocks: {
        $t: jest.fn().mockImplementation((msg) => msg),
        $tc: jest.fn().mockImplementation((msg) => msg),
      },
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("JobScmStatus", () => {
  it("no content if there is no scm status", async () => {
    const wrapper = await mountJobScmStatus({ itemData: {} });

    let detail = wrapper.findAll("*");
    expect(detail.length).toBe(0);
    expect(wrapper.text()).toEqual("");
  });
  it("spinner icon shown if loading prop is true", async () => {
    const wrapper = await mountJobScmStatus({ itemData: {}, loading: true });

    let detail = wrapper.findAll("span > i");
    expect(detail.length).toBe(1);
    expect(detail[0].classes()).toContain("fas");
    expect(detail[0].classes()).toContain("fa-spinner");
    expect(detail[0].classes()).toContain("fa-pulse");
    expect(wrapper.text()).toEqual("");
  });
});
