import { mount, VueWrapper } from "@vue/test-utils";
import { action } from "mobx";
import { Btn } from "uiv";
import OptionItem from "../OptionItem.vue";

const mountOptionItem = async (options: any): Promise<VueWrapper<any>> => {
  const wrapper = mount(OptionItem, {
    props: {
      ...options,
    },
    global: {
      mocks: {
        $t: (msg: string) => msg,
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

describe("OptionItem", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("non editable item has no controls", async () => {
    const wrapper = await mountOptionItem({
      option: { name: "a_test_option" },
      canMoveUp: false,
      canMoveDown: false,
      editable: false,
    });

    expect(wrapper.findAll(".btn").length).toBe(0);
    expect(wrapper.findAll(".btn-group").length).toBe(0);
  });
  it("editable item has controls", async () => {
    const wrapper = await mountOptionItem({
      option: { name: "a_test_option" },
      canMoveUp: false,
      canMoveDown: false,
      editable: true,
    });

    let findAll = wrapper.findAll(".btn");
    expect(findAll.length).toBe(6);
    expect(wrapper.findAll(".btn-group").length).toBe(2);
    console.log(findAll[0].attributes());
    expect(
      wrapper.findAll(".btn[title='option.view.action.edit.title']").length,
    ).toBe(1);
    expect(
      wrapper.findAll(".btn[title='option.view.action.duplicate.title']")
        .length,
    ).toBe(1);
    expect(
      wrapper.findAll(".btn[title='option.view.action.delete.title']").length,
    ).toBe(1);
  });
  it.each([true, false])(
    "move up button state %p",
    async (canMoveUp: boolean) => {
      const wrapper = await mountOptionItem({
        option: { name: "a_test_option" },
        canMoveUp,
        canMoveDown: false,
        editable: true,
      });

      let item = wrapper.find(".btn[title='option.view.action.moveUp.title']");
      expect(item).toBeTruthy();
      if (canMoveUp) {
        expect(item.attributes().disabled).toBeUndefined();
      } else {
        expect(item.attributes().disabled).not.toBeNull();
      }
    },
  );
  it.each([true, false])(
    "move down button state %p",
    async (canMoveDown: boolean) => {
      const wrapper = await mountOptionItem({
        option: { name: "a_test_option" },
        canMoveUp: false,
        canMoveDown,
        editable: true,
      });

      let item = wrapper.find(
        ".btn[title='option.view.action.moveDown.title']",
      );
      expect(item).toBeTruthy();
      if (canMoveDown) {
        expect(item.attributes().disabled).toBeUndefined();
      } else {
        expect(item.attributes().disabled).not.toBeNull();
      }
    },
  );
  it("editable item draggable disabled", async () => {
    const wrapper = await mountOptionItem({
      option: { name: "a_test_option" },
      canMoveUp: false,
      canMoveDown: false,
      editable: true,
    });

    let item = wrapper.find(".dragHandle");
    expect(item).toBeTruthy();
    expect(item.attributes().disabled).not.toBeNull();
  });
  it.each([
    [true, false],
    [false, true],
    [true, true],
  ])(
    "editable item draggable not disabled canMoveUp %p canMoveDown %p",
    async (canMoveUp: boolean, canMoveDown: boolean) => {
      const wrapper = await mountOptionItem({
        option: { name: "a_test_option" },
        canMoveUp,
        canMoveDown,
        editable: true,
      });

      let item = wrapper.find(".dragHandle");
      expect(item).toBeTruthy();
      expect(item.attributes().disabled).toBeUndefined();
    },
  );
  it.each(["edit", "duplicate", "delete", "moveUp", "moveDown"])(
    "emits %p event when button is clicked",
    async (action: string) => {
      let option = { name: "a_test_option" };
      const wrapper = await mountOptionItem({
        option,
        canMoveUp: true,
        canMoveDown: true,
        editable: true,
      });
      let editBtn = wrapper.findAll(
        `.btn[title='option.view.action.${action}.title']`,
      );
      expect(editBtn.length).toBe(1);
      await editBtn[0].trigger("click");
      expect(wrapper.emitted(action)).toBeTruthy();
      expect(wrapper.emitted(action)[0][0]).toStrictEqual(option);
    },
  );
  it("emits edit event when item view is clicked", async () => {
    let option = { name: "a_test_option" };
    const wrapper = await mountOptionItem({
      option,
      editable: true,
    });
    let content = wrapper.findAll(`.option-item-content`);
    expect(content.length).toBe(1);
    await content[0].trigger("click");
    expect(wrapper.emitted("edit")).toBeTruthy();
    expect(wrapper.emitted("edit")[0][0]).toStrictEqual(option);
  });
});
