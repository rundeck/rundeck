import { vi } from "vitest";
import { mount, VueWrapper } from "@vue/test-utils";
import OptionRemoteUrlConfig from "../OptionRemoteUrlConfig.vue";

vi.mock("@/library/rundeckService", () => ({
  getRundeckContext: vi.fn().mockImplementation(() => ({
    eventBus: { on: vi.fn(), emit: vi.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
    apiVersion: "44",
  })),
}));
vi.mock("../../../../../library/services/projects");

const mountOptionEdit = async (options: any): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionRemoteUrlConfig, {
    props: {
      ...options,
    },
    components: {},
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionRemoteUrlConfig", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    ["valuesUrl", { valuesUrl: "http://example.com" }],
    [
      "configRemoteUrl",
      { valuesUrl: "http://example.com" },
      "configRemoteUrl.jsonFilter",
    ],
  ])(
    "shows validation errors for field %p",
    async (
      fieldName: string,
      optData: any,
      errorName: string | null = null,
    ) => {
      const wrapper = await mountOptionEdit(
        Object.assign(
          {
            validationErrors: {
              [errorName || fieldName]: ["error1"],
            },
          },
          optData,
        ),
      );

      const section = wrapper.get(`[data-test="option-${fieldName}"]`);
      expect(section.classes()).toContain("has-error");
      const errorslist = section.get("div.help-block errorslist");
      expect(errorslist.attributes()["errors"]).toContain("error1");
    },
  );
});
