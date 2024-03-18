import { mount, VueWrapper } from "@vue/test-utils";
import OptionRemoteUrlConfig from "../OptionRemoteUrlConfig.vue";

const mountOptionEdit = async (options: any): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionRemoteUrlConfig, {
    props: {
      ...options,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
    },
    components: {},
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionRemoteUrlConfig", () => {
  afterEach(() => {
    jest.clearAllMocks();
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
    async (fieldName: string, optData: any, errorName: string = null) => {
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

      let section = wrapper.get(`[data-test=option.${fieldName}]`);
      expect(section.classes()).toContain("has-error");
      let errorslist = section.get("div.help-block errorslist");
      expect(errorslist.attributes()["errors"]).toContain("error1");
    },
  );
});
