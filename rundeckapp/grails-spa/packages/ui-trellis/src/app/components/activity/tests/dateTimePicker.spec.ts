import { shallowMount } from "@vue/test-utils";
import DateTimePicker from "../dateTimePicker.vue";
import moment from "moment";

describe("DateTimePicker.vue", () => {
  let wrapper;
  let now;

  beforeEach(() => {
    now = moment();
    wrapper = shallowMount(DateTimePicker, {
      propsData: {
        modelValue: moment().format(),
        dateClass: "date-class",
        timeClass: "time-class",
      },
    });
  });
  it("sets initial data correctly", () => {
    expect(wrapper.vm.dateString).toBe(now.format("YYYY-MM-DD"));
    expect(wrapper.vm.time).toBeInstanceOf(Date);
  });

  // it("emits update:modelValue when time changes", async () => {
  //   const newTime = new Date();
  //   const newTimeFormatted = moment(newTime).format();
  //   await wrapper.setData({ time: newTime });
  //
  //   expect(wrapper.emitted("update:modelValue")).toBeTruthy();
  //   expect(wrapper.emitted("update:modelValue")[0]).toEqual([newTimeFormatted]);
  // });
  it("renders date-picker and time-picker", () => {
    expect(wrapper.find(".bs-date-picker").exists()).toBe(true);
    expect(wrapper.find(".time-class").exists()).toBe(true);
  });

  it("sets initial data correctly", () => {
    expect(wrapper.vm.dateString).toBe(moment().format("YYYY-MM-DD"));
    expect(wrapper.vm.time).toBeInstanceOf(Date);
  });

  it("emits update:modelValue when time changes", async () => {
    const newTime = new Date();
    await wrapper.setData({ time: newTime });

    expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    expect(wrapper.emitted("update:modelValue")[0]).toEqual([
      moment(newTime).format(),
    ]);
  });

  it("updates time and dateString when modelValue changes", async () => {
    const newModelValue = moment().add(1, "days").format();
    await wrapper.setProps({ modelValue: newModelValue });

    expect(wrapper.vm.dateString).toBe(
      moment(newModelValue).format("YYYY-MM-DD"),
    );
    expect(wrapper.vm.time).toEqual(moment(newModelValue).toDate());
  });
  it("recalculates date when dateString changes", async () => {
    const newDateString = moment().add(1, "days").format("YYYY-MM-DD");
    await wrapper.setData({ dateString: newDateString });

    expect(moment(wrapper.vm.time).format("YYYY-MM-DD")).toBe(newDateString);
  });

  it("sets time and dateString from modelValue when modelValue changes", async () => {
    const newModelValue = moment().add(1, "days").format();
    await wrapper.setProps({ modelValue: newModelValue });

    expect(wrapper.vm.time).toEqual(moment(newModelValue).toDate());
    expect(wrapper.vm.dateString).toBe(
      moment(newModelValue).format("YYYY-MM-DD"),
    );
  });
  it("binds v-model and class correctly to date-picker and time-picker", () => {
    expect(typeof wrapper.vm.dateString).toBe("string");
    expect(wrapper.vm.time).toBeInstanceOf(Date);
    expect(wrapper.vm.dateClass).toBe("date-class");
    expect(wrapper.vm.timeClass).toBe("time-class");
  });
});
