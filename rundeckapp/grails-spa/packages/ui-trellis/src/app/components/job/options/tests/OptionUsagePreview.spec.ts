import { mount, VueWrapper } from "@vue/test-utils";
import OptionUsagePreview from "../OptionUsagePreview.vue";

const mountOptionUsagePreview = async (
  options: any,
): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionUsagePreview, {
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

describe("OptionUsagePreview", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("file option shows variables", async () => {
    const wrapper = await mountOptionUsagePreview({
      option: { name: "test_name", optionType: "file" },
      validationErrors: {},
    });
    const sect = wrapper.get("section");
    expect(sect.html()).toContain(`\${file.test_name}`);
    expect(sect.html()).toContain(`@file.test_name@`);
    expect(sect.html()).toContain(`$RD_FILE_TEST_NAME`);
    expect(sect.html()).toContain(`\${file.test_name.fileName}`);
    expect(sect.html()).toContain(`@file.test_name.fileName@`);
    expect(sect.html()).toContain(`$RD_FILE_TEST_NAME_FILENAME`);
    expect(sect.html()).toContain(`\${file.test_name.sha}`);
    expect(sect.html()).toContain(`@file.test_name.sha@`);
    expect(sect.html()).toContain(`$RD_FILE_TEST_NAME_SHA`);
  });
  it.each([
    { name: "test_name", optionType: "text" },
    { name: "test_name", optionType: "text", secure: true, valueExposed: true },
  ])("plain option shows variables", async (option: any) => {
    const wrapper = await mountOptionUsagePreview({
      option,
      validationErrors: {},
    });
    const sect = wrapper.get("section");
    expect(sect.html()).toContain(`\${option.test_name}`);
    expect(sect.html()).toContain(`\${unquotedoption.test_name}`);
    expect(sect.html()).toContain(`@option.test_name@`);
    expect(sect.html()).toContain(`$RD_OPTION_TEST_NAME`);
  });
  it("plain secure option does not show variables", async () => {
    const wrapper = await mountOptionUsagePreview({
      option: {
        name: "test_name",
        optionType: "text",
        secure: true,
        valueExposed: false,
      },
      validationErrors: {},
    });
    const sect = wrapper.get("section");
    expect(sect.html()).not.toContain(`\${option.test_name}`);
    expect(sect.html()).not.toContain(`\${unquotedoption.test_name}`);
    expect(sect.html()).not.toContain(`@option.test_name@`);
    expect(sect.html()).not.toContain(`$RD_OPTION_TEST_NAME`);
    expect(sect.html()).toContain("form.option.usage.secureAuth.message");
  });
  it.each([
    { option: { name: "", optionType: "text" }, validationErrors: {} },
    {
      option: { name: "in valid", optionType: "text" },
      validationErrors: { name: ["invalid"] },
    },
  ])("invalid option name shows nothing %p", async (options: any) => {
    const wrapper = await mountOptionUsagePreview(options);
    const sect = wrapper.findAll("section");
    expect(sect.length).toBe(0);
  });
});
