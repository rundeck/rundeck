import { mount } from "@vue/test-utils";
import DateTimePicker from "../dateTimePicker.vue";
import moment from "moment";
const mountDateTimePicker = async (props = {}) => {
  return mount(DateTimePicker, {
    props: {
      modelValue: moment().format(),
      dateClass: "date-class",
      timeClass: "time-class",
      ...props,
    },
    global: {
      stubs: {
        "date-picker": true,
        "time-picker": true,
      },
    },
  });
};
describe("DateTimePicker.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("emits update:modelValue when time changes", async () => {
    const wrapper = await mountDateTimePicker();
    const newTime = new Date();
    const newTimeFormatted = moment(newTime).format();
    const timePicker = wrapper.findComponent({ name: "time-picker" });
    timePicker.vm.$emit("update:modelValue", newTime);
    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted[0]).toEqual([newTimeFormatted]);
  });
  it("updates time and dateString when modelValue changes", async () => {
    const wrapper = await mountDateTimePicker();
    const newModelValue = moment().add(1, "days").format();
    await wrapper.setProps({ modelValue: newModelValue });
    expect(wrapper.vm.dateString).toBe(
      moment(newModelValue).format("YYYY-MM-DD")
    );
    expect(wrapper.vm.time).toEqual(moment(newModelValue).toDate());
  });
  it("recalculates date when dateString changes", async () => {
    const wrapper = await mountDateTimePicker();
    const newDateString = moment().add(1, "days").format("YYYY-MM-DD");
    await wrapper.setData({ dateString: newDateString });
    expect(moment(wrapper.vm.time).format("YYYY-MM-DD")).toBe(newDateString);
  });
  it("binds v-model, class correctly and sets correct attributes on date-picker and time-picker", async () => {
    const wrapper = await mountDateTimePicker();
    const datePicker = wrapper.findComponent({ name: "date-picker" });
    const timePicker = wrapper.findComponent({ name: "time-picker" });
    // Check v-model and class for date-picker
    expect(datePicker.attributes("modelvalue")).toBe(wrapper.vm.dateString);
    expect(datePicker.attributes("class")).toContain(wrapper.vm.dateClass);
    expect(datePicker.attributes("clear-btn")).toBe("false");
    // Check v-model and class for time-picker
    const receivedTime = new Date(
      timePicker.attributes("modelvalue")
    ).toISOString();
    const expectedTime = new Date(wrapper.vm.time).toISOString();
    expect(receivedTime).toBe(expectedTime);
    expect(timePicker.attributes("class")).toContain(wrapper.vm.timeClass);
    // Check CSS classes and props
    expect(wrapper.find(".date-class").exists()).toBe(true);
    expect(wrapper.find(".time-class").exists()).toBe(true);
    expect(wrapper.find(".bs-date-picker").exists()).toBe(true);
  });
  it("computes datetime correctly", async () => {
    const wrapper = await mountDateTimePicker();
    expect(wrapper.vm.datetime).toBe(moment(wrapper.vm.time).format());
  });
  it("handles edge cases", async () => {
    const leapYear = "2024-02-29";
    const wrapper = await mountDateTimePicker({ modelValue: leapYear });
    expect(wrapper.vm.dateString).toBe(leapYear);
    expect(wrapper.vm.time).toBeInstanceOf(Date);
  });
  it("is accessible", async () => {
    const wrapper = await mountDateTimePicker();
    const datePicker = wrapper.findComponent({ name: "date-picker" });
    const timePicker = wrapper.findComponent({ name: "time-picker" });
    expect(datePicker.attributes("role")).toBe("combobox");
    expect(timePicker.attributes("role")).toBe("combobox");
  });
});
