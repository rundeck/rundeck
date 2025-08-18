import { mount, shallowMount, VueWrapper } from "@vue/test-utils";
import { VueNode } from "@vue/test-utils/dist/types";
import { Btn } from "uiv";
import { validateJobOption } from "../../../../../library/services/jobEdit";
import OptionEdit from "../OptionEdit.vue";

jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
    projectName: "testProject",
  })),
}));
jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));
// Mock validateJobOption method
jest.mock("@/library/services/jobEdit");

const mockedValidateJobOption = validateJobOption as jest.MockedFunction<
  typeof validateJobOption
>;

const mountOptionEdit = async (options: any): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionEdit, {
    props: {
      ...options,
    },

    components: {
      Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};
const shallowMountOptionEdit = async (
  options: any,
): Promise<VueWrapper<any>> => {
  const wrapper = shallowMount(OptionEdit, {
    props: {
      ...options,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
      },
      stubs: {
        PluginConfig: {
          name: "PluginConfig",
          props: [
            "mode",
            "serviceName",
            "modelValue",
            "provider",
            "showTitle",
            "showDescription",
            "scope",
          ],
          template:
            "<div data-test-component='pluginConfig' :data-test-svc-name='serviceName' :data-test-provider='provider'>" +
            "Plugin Config Component" +
            "</div>",
        },
      },
    },
    components: {
      Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionEdit", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it.each([
    [false, "pluginType", false],
    [true, null, false],
    [true, "", false],
    [true, "pluginType", true],
  ])(
    "file select option presence if file plugin enabled %p and pluginType %p expect %p",
    async (enabled: boolean, pluginType: any, expected: boolean) => {
      const wrapper = await shallowMountOptionEdit({
        features: { fileUploadPlugin: enabled },
        fileUploadPluginType: pluginType,
      });

      const sel = wrapper.get("#opttype_");
      expect(sel.findAll("option").length).toBe(expected ? 2 : 1);
      expect(sel.findAll("option[value=text]").length).toBe(1);
      expect(sel.findAll("option[value=file]").length).toBe(expected ? 1 : 0);
    },
  );
  it("file option type shows file plugin config", async () => {
    const wrapper = await mountOptionEdit({
      modelValue: { name: "a_test_option", type: "file" },
      editable: true,
      features: { fileUploadPlugin: true },
      fileUploadPluginType: "pluginType",
    });

    let element = wrapper.get(
      ".form-group plugin-config[servicename=FileUpload][provider=pluginType]",
    );
  });
  it.each([
    ["plain", {}],
    ["date", { isDate: true }],
    ["secure", { secure: true }],
    ["secureExposed", { secure: true, valueExposed: true }],
  ])(
    "infers input type %p selection from option data",
    async (field: string, data: any) => {
      const inputtypes = ["plain", "date", "secure", "secureExposed"];
      const wrapper = await mountOptionEdit({
        modelValue: Object.assign({ name: "test", type: "text" }, data),
        editable: true,
      });
      //check all radio buttons
      inputtypes.forEach((type) => {
        let inputRadio = wrapper.get(`input[type=radio][value=${type}]`);
        expect((inputRadio.element as VueNode<HTMLInputElement>).checked).toBe(
          type === field,
        );
      });
    },
  );
  it.each([
    ["#inputplain_", "plain", false, false, false],
    ["#inputdate_", "date", true, false, false],
    ["#sectrue_", "secureExposed", false, true, true],
    ["#secexpfalse_", "secure", false, true, false],
  ])(
    "change input type selection %p %p %p %p %p %p",
    async (
      selector: string,
      inputType: string,
      isDate: boolean,
      secure: boolean,
      valueExposed: boolean,
    ) => {
      mockedValidateJobOption.mockResolvedValueOnce({
        valid: true,
      });
      const wrapper = await mountOptionEdit({
        modelValue: { name: "test", type: "text" },
        editable: true,
      });

      let plainType = wrapper.get(selector);
      await plainType.setValue();
      //save
      await wrapper.vm.$nextTick();
      await wrapper.vm.doSave();
      expect(wrapper.vm.hasFormErrors).toBeFalsy();
      //test emitted
      let actual = wrapper.emitted();
      expect(actual["update:modelValue"]).toBeTruthy();
      expect(actual["update:modelValue"].length).toBe(1);
      let emittedOption = actual["update:modelValue"][0][0];
      expect(emittedOption).toEqual({
        name: "test",
        type: "text",
        description: "",
        inputType,
        hidden: false,
        multivalueAllSelected: false,
        multivalued: false,
        isDate,
        secure,
        valueExposed,
        required: false,
        sortValues: false,
        value: "",
        valuesType: "list",
      });
    },
  );
  it.each([
    ["#vtrlist_", "list", {}],
    [
      "#vtrurl_",
      "url",
      {
        valuesUrl: "",
        remoteUrlAuthenticationType: "",
        configRemoteUrl: {},
      },
    ],
    [
      "#optvalplugin_testplugin",
      "testplugin",
      { optionValuesPluginType: "testplugin" },
    ],
  ])(
    "change values type selection %p %p",
    async (selector: string, valuesType: string, data: any) => {
      mockedValidateJobOption.mockResolvedValueOnce({
        valid: true,
      });
      const wrapper = await mountOptionEdit({
        modelValue: { name: "test", type: "text" },
        features: { optionValuesPlugin: true },
        optionValuesPlugins: [
          { name: "testplugin", description: "", title: "Test Plugin" },
        ],
        editable: true,
      });

      let valuesTypeRadio = wrapper.get(selector);
      await valuesTypeRadio.setValue();
      //save
      await wrapper.vm.$nextTick();
      await wrapper.vm.doSave();
      expect(wrapper.vm.hasFormErrors).toBeFalsy();
      //test emitted
      let actual = wrapper.emitted();
      expect(actual["update:modelValue"]).toBeTruthy();
      expect(actual["update:modelValue"].length).toBe(1);
      let emittedOption = actual["update:modelValue"][0][0];
      expect(emittedOption).toEqual(
        Object.assign(
          {
            name: "test",
            type: "text",
            description: "",
            inputType: "plain",
            hidden: false,
            multivalueAllSelected: false,
            multivalued: false,
            isDate: false,
            secure: false,
            valueExposed: false,
            required: false,
            sortValues: false,
            value: "",
            valuesType,
          },
          data,
        ),
      );
    },
  );
  it("doSave does not emit value if validation fails", async () => {
    mockedValidateJobOption.mockResolvedValueOnce({
      valid: false,
      message: "",
      messages: { name: ["Wrong value"] },
    });
    const wrapper = await mountOptionEdit({
      modelValue: { name: "test", type: "text" },
      features: { optionValuesPlugin: true },
      optionValuesPlugins: [
        { name: "testplugin", description: "", title: "Test Plugin" },
      ],
      editable: true,
    });

    await wrapper.vm.doSave();
    expect(wrapper.vm.validationErrors).toStrictEqual({
      name: ["Wrong value"],
    });
    expect(wrapper.vm.hasFormErrors).toBeTruthy();
    expect(wrapper.vm.hasError("name")).toBeTruthy();
    expect(wrapper.emitted()).not.toHaveProperty("update:modelValue");
  });
  it.each([true, false])(
    "validate job option jobWasScheduled %p param is used",
    async (jobWasScheduled: boolean) => {
      mockedValidateJobOption.mockResolvedValueOnce({
        valid: true,
        message: "",
        messages: {},
      });
      const wrapper = await mountOptionEdit({
        modelValue: { name: "test", type: "text" },
        features: { optionValuesPlugin: true },
        jobWasScheduled,
        optionValuesPlugins: [
          { name: "testplugin", description: "", title: "Test Plugin" },
        ],
        editable: true,
      });

      await wrapper.vm.doSave();
      expect(mockedValidateJobOption).toHaveBeenCalledWith(
        "testProject",
        jobWasScheduled,
        expect.objectContaining({
          name: "test",
          type: "text",
        }),
      );
    },
  );
  it.each([
    ["", "has-warning", "form.field.required.message"],
    ["in valid", "has-error", "form.option.regex.validation.error"],
  ])(
    "shows error messages for invalid name field",
    async (value: string, cls: string, msg: string) => {
      const wrapper = await mountOptionEdit({
        modelValue: { name: "", type: "text" },
        editable: true,
      });
      let optname = wrapper.get("#optname_");
      await optname.setValue(value);
      await optname.trigger("blur");
      await wrapper.vm.$nextTick();
      let section = wrapper.get("[data-test=option.name]");
      expect(section.classes()).toContain(cls);
      let errorslist = section.get("div.help-block errorslist");
      expect(errorslist.attributes()["errors"]).toContain(msg);
    },
  );
  it.each([
    ["option.label", "#opt_label", 255],
    ["option.name", "#optname_", 255],
  ])(
    "shows error messages for field %p %p longer than %p",
    async (sectionName: string, id: string, len: number) => {
      const wrapper = await mountOptionEdit({
        modelValue: { name: "aname", type: "text" },
        editable: true,
      });
      let field = wrapper.get(id);
      //set to max length
      await field.setValue("a".repeat(len));
      await field.trigger("blur");
      await wrapper.vm.$nextTick();
      let section = wrapper.get(`[data-test=${sectionName}]`);
      expect(section.classes()).not.toContain("has-error");
      expect(section.find("div.help-block errorslist").exists()).toBeFalsy();

      //1 more than max
      await field.setValue("a".repeat(len) + "a");
      await field.trigger("blur");
      await wrapper.vm.$nextTick();

      expect(section.classes()).toContain("has-error");
      let errorslist = section.get("div.help-block errorslist");
      expect(errorslist.attributes()["errors"]).toContain(
        "form.field.too.long.message",
      );
    },
  );
  it.each([
    ["label", {}],
    ["name", {}],
    ["description", {}],
    ["value", {}],
    ["storagePath", { secure: true }],
    ["values", {}],
    ["valuesListDelimiter", {}],
    ["regex", { regex: "asdf" }],
    ["hidden", {}],
    ["required", {}],
    ["delimiter", { multivalued: true }],
  ])(
    "shows validation errors for field %p",
    async (fieldName: string, optData: any, errorName: string = null) => {
      const wrapper = await mountOptionEdit({
        modelValue: Object.assign({ name: "aname", type: "text" }, optData),
        editable: true,
      });
      wrapper.vm.addError(errorName || fieldName, "error1");
      await wrapper.vm.$nextTick();
      let section = wrapper.get(`[data-test=option.${fieldName}]`);
      expect(section.classes()).toContain("has-error");
      let errorslist = section.get("div.help-block errorslist");
      expect(errorslist.attributes()["errors"]).toContain("error1");
    },
  );
});
