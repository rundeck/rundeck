import { mount, VueWrapper } from "@vue/test-utils";
import { Btn } from "uiv";
import { JobOptionsData } from "../../../../../library/types/jobs/JobEdit";
import OptionsEditor from "../OptionsEditor.vue";

// Mock pluginService methods
jest.mock("@/library/modules/pluginService", () => ({
  getPluginProvidersForService: jest.fn().mockResolvedValue({
    service: "OptionValues",
    descriptions: [],
    labels: [],
  }),
}));
jest.mock("@/library/modules/rundeckClient", () => ({
  client: {},
}));
jest.mock("@/library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));
jest.mock("@/library/rundeckService.ts", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    eventBus: { on: jest.fn(), emit: jest.fn() },
    rdBase: "http://localhost:4440",
  })),
}));

const mountOptionsEditor = async (options: {
  optionsData: JobOptionsData;
  edit: boolean;
}): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionsEditor, {
    props: {
      optionsData: options.optionsData,
      edit: options.edit,
    },
    global: {
      mocks: {
        $t: jest.fn().mockImplementation((msg) => msg),
        $tc: jest.fn().mockImplementation((msg) => msg),
      },
      stubs: {
        Draggable: {
          name: "Draggable",
          props: ["modelValue"],
          defineEmits: ["update:modelValue", "update"],
          template:
            "<div data-test='draggable-stub'>" +
            "<span v-for='(opt,i) in modelValue'>" +
            "<slot name='item' :element='opt' :index='i'></slot>" +
            "</span>" +
            "Footer" +
            "</div>",
        },
        UndoRedo: {
          name: "UndoRedo",
          props: ["eventBus"],
          template: "<div></div>",
        },
        OptionItem: {
          name: "OptionItem",
          props: ["editable", "canMoveUp", "canMoveDown", "option"],
          defineEmits: ["moveUp", "moveDown", "edit", "delete", "duplicate"],
          template: "<div>OptionItem: {{option.name}}</div>",
        },
        OptionEdit: {
          name: "OptionEdit",
          props: [
            "error",
            "newOption",
            "modelValue",
            "fileUploadPluginType",
            "features",
            "optionValuesPlugins",
            "errors",
            "uiFeatures",
          ],
          defineEmits: ["update:modelValue", "cancel"],
          template: "<div>OptionEdit: {{option.name}}</div>",
        },
        PluginConfig: true,
      },
    },

    components: {
      btn: Btn,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("OptionsEditor", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("edit shows add button", async () => {
    let optionsData = {
      features: { feature1: true, feature2: false },
      fileUploadPluginType: "fileUploadPluginType",
      options: [],
    } as JobOptionsData;
    const wrapper = await mountOptionsEditor({ optionsData, edit: true });
    let btn = wrapper.get(".ready");
    expect(btn.attributes().title).toEqual("add.new.option");
    expect(btn.text()).toEqual("add.an.option");
    let icon = btn.get(".glyphicon.glyphicon-plus");
    expect(icon).toBeDefined();
  });
  it("shows empty list text", async () => {
    let optionsData = {
      features: { feature1: true, feature2: false },
      fileUploadPluginType: "fileUploadPluginType",
      options: [],
    } as JobOptionsData;
    const wrapper = await mountOptionsEditor({ optionsData, edit: true });
    let btn = wrapper.get(".empty.note");
    expect(btn.text()).toEqual("no.options.message");
  });
  it("shows list of options", async () => {
    let options = [
      {
        name: "option1",
        optionType: "text",
        inputType: "plain",
      },
      {
        name: "option2",
        optionType: "file",
        inputType: "plain",
      },
    ];
    let optionsData = {
      features: { feature1: true, feature2: false },
      fileUploadPluginType: "fileUploadPluginType",
      options,
    } as JobOptionsData;
    const wrapper = await mountOptionsEditor({ optionsData, edit: true });
    let draggable = wrapper.get("[data-test=draggable-stub]");
    let spans = draggable.findAll("span");
    expect(spans).toHaveLength(2);
    expect(spans[0].text()).toContain("OptionItem: option1");
    expect(spans[1].text()).toContain("OptionItem: option2");

    expect(wrapper.find(".empty.note").exists()).toEqual(false);
  });
});
