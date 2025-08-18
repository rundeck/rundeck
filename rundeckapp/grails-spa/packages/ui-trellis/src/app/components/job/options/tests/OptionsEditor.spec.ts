import { mount, VueWrapper } from "@vue/test-utils";
import { Btn } from "uiv";
import { JobOptionsData } from "../../../../../library/types/jobs/JobEdit";
import { Operation } from "../model/ChangeEvents";
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
const expectOptionOrder = (wrapper: VueWrapper<any>, order: string[]) => {
  const draggable = wrapper.get("[data-test=draggable-stub]");
  const spans = draggable.findAll("[data-test-component=OptionItemComponent]");
  expect(spans).toHaveLength(order.length);
  for (let i = 0; i < order.length; i++) {
    expect(spans[i].text()).toContain(`OptionItem: ${order[i]}`);
  }

  expect(wrapper.find(".empty.note").exists()).toEqual(false);
};
/**
 * Mounts the OptionsEditor component with basic options
 * @param names - list of option names
 */
const mountBasicOptionsEditor = async (
  names: string[] = ["option1", "option2"],
): Promise<VueWrapper<any>> => {
  const options = names.map((name) => {
    return {
      name,
      type: "text",
      inputType: "plain",
    };
  });
  const optionsData = {
    features: { feature1: true, feature2: false },
    fileUploadPluginType: "fileUploadPluginType",
    jobWasScheduled: false,
    options,
  } as JobOptionsData;
  return mountOptionsEditor({ optionsData, edit: true });
};
const mountOptionsEditor = async (options: {
  optionsData: JobOptionsData;
  edit: boolean;
}): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionsEditor, {
    props: {
      optionsData: Object.assign(
        { jobWasScheduled: false },
        options.optionsData,
      ),
      edit: options.edit,
    },
    global: {
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
            "<div data-test='draggable-footer'><slot name='footer'></slot></div>" +
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
          template:
            "<div data-test-component='OptionItemComponent'>OptionItem: {{option.name}}</div>",
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
            "jobWasScheduled",
          ],
          defineEmits: ["update:modelValue", "cancel"],
          template:
            "<div data-test-component='OptionEditComponent' :data-job-was-scheduled='jobWasScheduled'>OptionEdit: {{modelValue.name}}</div>",
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
    const optionsData = {
      features: { feature1: true, feature2: false },
      fileUploadPluginType: "fileUploadPluginType",
      jobWasScheduled: false,
      options: [],
    } as JobOptionsData;
    const wrapper = await mountOptionsEditor({ optionsData, edit: true });
    const btn = wrapper.get(".ready");
    expect(btn.attributes().title).toEqual("add.new.option");
    expect(btn.text()).toEqual("add.an.option");
    const icon = btn.get(".glyphicon.glyphicon-plus");
    expect(icon).toBeDefined();
  });

  it("shows empty list text", async () => {
    const optionsData = {
      features: { feature1: true, feature2: false },
      fileUploadPluginType: "fileUploadPluginType",
      jobWasScheduled: false,
      options: [],
    } as JobOptionsData;
    const wrapper = await mountOptionsEditor({ optionsData, edit: true });
    const btn = wrapper.get(".empty.note");
    expect(btn.text()).toEqual("no.options.message");
  });
  it("shows list of options", async () => {
    const wrapper = await mountBasicOptionsEditor();

    expectOptionOrder(wrapper, ["option1", "option2"]);
  });
  it.each([
    [0, "option2"],
    [1, "option1"],
  ])(
    "remove option %p removes the option leaving %p remaining",
    async (index: number, remaining: string) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.doRemove(index);
      await wrapper.vm.$nextTick();

      expectOptionOrder(wrapper, [remaining]);
    },
  );
  it.each([
    [0, "option2"],
    [1, "option1"],
  ])(
    "operation remove %p removes the option leaving %p remaining",
    async (index: number, remaining: string) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.operationRemove(index);
      await wrapper.vm.$nextTick();

      expectOptionOrder(wrapper, [remaining]);
    },
  );
  it.each([
    [0, ["option1", "option2"]],
    [1, ["option2", "option1"]],
  ])(
    "doMoveUp index %p result in %p",
    async (index: number, result: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.doMoveUp(index);
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, result);
    },
  );
  it.each([
    [0, ["option2", "option1"]],
    [1, ["option1", "option2"]],
  ])(
    "doMoveDown index %p result in %p",
    async (index: number, result: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.doMoveDown(index);
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, result);
    },
  );
  it.each([
    [0, 1, ["option2", "option1", "option3"]],
    [0, 2, ["option2", "option3", "option1"]],
    [1, 2, ["option1", "option3", "option2"]],
    [1, 0, ["option2", "option1", "option3"]],
    [2, 0, ["option3", "option1", "option2"]],
    [2, 1, ["option1", "option3", "option2"]],
  ])(
    "operation move index %p dest %p result in %p",
    async (index: number, dest: number, result: string[]) => {
      const wrapper = await mountBasicOptionsEditor([
        "option1",
        "option2",
        "option3",
      ]);
      wrapper.vm.operationMove(index, dest);
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, result);
    },
  );
  it.each([
    [0, ["newoption", "option1", "option2"]],
    [1, ["option1", "newoption", "option2"]],
    [2, ["option1", "option2", "newoption"]],
  ])(
    "operation insert index %p result in %p",
    async (index: number, result: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.operationInsert(index, {
        name: "newoption",
        type: "text",
        inputType: "plain",
      });
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, result);
    },
  );
  it.each([0, 1])(
    "shows edit form for selected edit item %p",
    async (selected: number) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.doEdit(selected);
      await wrapper.vm.$nextTick();
      const draggable = wrapper.get("[data-test=draggable-stub]");
      const spans = draggable.findAll("div[data-test-component]");
      expect(spans).toHaveLength(2);
      expect(spans[0].text()).toContain(
        selected == 0 ? "OptionEdit: option1" : "OptionItem: option1",
      );
      expect(spans[1].text()).toContain(
        selected == 1 ? "OptionEdit: option2" : "OptionItem: option2",
      );

      expect(wrapper.find(".empty.note").exists()).toEqual(false);
    },
  );
  it.each([true, false])(
    "option edit prop jobWasScheduled value %p",
    async (jobWasScheduled: boolean) => {
      const propVals = {
        optionsData: {
          features: {
            feature1: true,
            feature2: false,
          },
          fileUploadPluginType: "fileUploadPluginType",
          jobWasScheduled,
          options: [
            {
              name: "option1",
              type: "text",
              inputType: "plain",
            },
          ],
        },
        edit: true,
      };
      const wrapper = await mountOptionsEditor(propVals);
      wrapper.vm.doEdit(0);
      await wrapper.vm.$nextTick();
      const draggable = wrapper.get("[data-test=draggable-stub]");
      const spans = draggable.findAll("div[data-test-component]");
      expect(spans).toHaveLength(1);
      expect(spans[0].text()).toContain("OptionEdit: option1");
      expect(spans[0].attributes("data-job-was-scheduled")).toEqual(
        jobWasScheduled.toString(),
      );
      propVals.optionsData.jobWasScheduled = !jobWasScheduled;
      //set prop value
      await wrapper.setProps(propVals);
      expect(spans[0].attributes("data-job-was-scheduled")).toEqual(
        (!jobWasScheduled).toString(),
      );
    },
  );
  it("create new option shows edit form", async () => {
    const wrapper = await mountBasicOptionsEditor();
    wrapper.vm.optaddnew();
    await wrapper.vm.$nextTick();
    const draggable = wrapper.get("[data-test=draggable-footer]");
    const item = draggable.get("[data-test-component=OptionEditComponent]");
    expect(item.text()).toEqual("OptionEdit:");
  });
  it.each([
    [0, ["newname", "option2"]],
    [1, ["option1", "newname"]],
  ])(
    "update option modifies option",
    async (index: number, newnames: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.updateOption(index, {
        name: "newname",
        type: "text",
        inputType: "plain",
      });
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, newnames);
    },
  );
  it.each([
    [0, ["newname", "option2"]],
    [1, ["option1", "newname"]],
  ])(
    "operation modify modifies option %p to %p",
    async (index: number, newnames: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.operationModify(index, {
        name: "newname",
        type: "text",
        inputType: "plain",
      });
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, newnames);
    },
  );
  it("save new option appends option", async () => {
    const wrapper = await mountBasicOptionsEditor();
    wrapper.vm.saveNewOption({
      name: "newname",
      type: "text",
      inputType: "plain",
    });
    await wrapper.vm.$nextTick();
    expectOptionOrder(wrapper, ["option1", "option2", "newname"]);
  });
  it.each([
    [0, ["newname", "option1", "option2"]],
    [1, ["option1", "newname", "option2"]],
    [2, ["option1", "option2", "newname"]],
  ])(
    "operation insert %p modifies list result %p",
    async (index: number, order: string[]) => {
      const wrapper = await mountBasicOptionsEditor();
      await wrapper.vm.operationInsert(index, {
        name: "newname",
        type: "text",
        inputType: "plain",
      });
      await wrapper.vm.$nextTick();
      expectOptionOrder(wrapper, order);
    },
  );
  it.each([
    ["somename1", "somename1_copy"],
    ["somename2", "somename2_copy"],
  ])(
    "duplicate option shows edit form with new name %p to %p",
    async (orig: string, newname: string) => {
      const options = [
        {
          name: orig,
          type: "text",
          inputType: "plain",
        },
      ];
      const optionsData = {
        features: { feature1: true, feature2: false },
        fileUploadPluginType: "fileUploadPluginType",
        jobWasScheduled: false,
        options,
      } as JobOptionsData;
      const wrapper = await mountOptionsEditor({ optionsData, edit: true });
      wrapper.vm.doDuplicate(0);
      await wrapper.vm.$nextTick();
      const draggable = wrapper.get("[data-test=draggable-footer]");
      const item = draggable.get("[data-test-component=OptionEditComponent]");
      expect(item.text()).toEqual(`OptionEdit: ${newname}`);
    },
  );
  it.each([
    [Operation.Insert, "operationInsert"],
    [Operation.Modify, "operationModify"],
    [Operation.Move, "operationMove"],
    [Operation.Remove, "operationRemove"],
  ])(
    "operation %p calls correct method %p",
    async (op: Operation, method: string) => {
      const spy = jest
        .spyOn(OptionsEditor.methods, method)
        .mockImplementation(() => {});
      const indexMock = jest.spyOn(OptionsEditor.methods, "updateIndexes");
      const wrapper = await mountBasicOptionsEditor();
      wrapper.vm.operation(op, { index: 0, operation: op, undo: op });
      await wrapper.vm.$nextTick();
      expect(spy).toHaveBeenCalled();
      expect(indexMock).toHaveBeenCalled();
      spy.mockRestore();
      indexMock.mockRestore();
    },
  );
  it("mount calls updateIndexes", async () => {
    const indexMock = jest.spyOn(OptionsEditor.methods, "updateIndexes");
    const wrapper = await mountBasicOptionsEditor();
    expect(indexMock).toHaveBeenCalled();
    indexMock.mockRestore();
  });
  it("sortIndex is updated after doRemove", async () => {
    const wrapper = await mountBasicOptionsEditor();
    //expect sortIndex to be set correctly after mount
    const intOptions = wrapper.vm.intOptions;
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
    //remove first item
    wrapper.vm.doRemove(0);
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1]);
  });
  it("sortIndex is updated after doMoveUp", async () => {
    const wrapper = await mountBasicOptionsEditor();
    //expect sortIndex to be set correctly after mount
    const intOptions = wrapper.vm.intOptions;
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
    //remove first item
    wrapper.vm.doMoveUp(1);
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
  });
  it("sortIndex is updated after doMoveDown", async () => {
    const wrapper = await mountBasicOptionsEditor();
    //expect sortIndex to be set correctly after mount
    const intOptions = wrapper.vm.intOptions;
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
    //remove first item
    wrapper.vm.doMoveDown(0);
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
  });
  it("sortIndex is updated after saveNewOption", async () => {
    const wrapper = await mountBasicOptionsEditor();
    //expect sortIndex to be set correctly after mount
    const intOptions = wrapper.vm.intOptions;
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2]);
    //remove first item
    wrapper.vm.saveNewOption({
      name: "newoption",
      type: "text",
      inputType: "plain",
    });
    expect(intOptions.map((item) => item.sortIndex)).toEqual([1, 2, 3]);
  });
});
