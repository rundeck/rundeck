import { mount, VueWrapper } from "@vue/test-utils";
import DateTimePicker from "../dateTimePicker.vue";
import moment from "moment";
import { ComponentPublicInstance } from "vue";

interface DateTimePickerProps {
  modelValue: string | Date;
  dateClass?: string;
  timeClass?: string;
}

interface DateTimePickerInstance extends ComponentPublicInstance {
  dateString: string;
  time: Date;
  datetime: string;
  setFromValue: () => void;
  recalcDate: () => void;
}

const initialTime = new Date("2024-06-30T13:00:00");
const newTime = new Date("2024-07-01T07:00:00");
const mountDateTimePicker = async (
  props: Partial<DateTimePickerProps> = {},
) => {
  return mount(DateTimePicker, {
    props: {
      modelValue: initialTime,
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
  }) as unknown as VueWrapper<DateTimePickerInstance>;
};
describe("DateTimePicker.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });
  it("emits update:modelValue when time changes", async () => {
    const wrapper = await mountDateTimePicker();

    const newTimeFormatted = moment(newTime).format();
    const timePicker = wrapper.findComponent({ name: "time-picker" });
    timePicker.vm.$emit("update:modelValue", newTime);
    await wrapper.vm.$nextTick();

    const emitted = wrapper.emitted("update:modelValue");
    expect(emitted.pop()).toEqual([newTimeFormatted]);
  });
  it("updates time and dateString when modelValue changes", async () => {
    const wrapper = await mountDateTimePicker();
    const newModelValue = moment(newTime).add(1, "days").format();
    await wrapper.setProps({ modelValue: newModelValue });
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.dateString).toBe("2024-07-02");
    expect(wrapper.vm.time).toEqual(moment(newModelValue).toDate());
  });
  it("recalculates date when dateString changes", async () => {
    const wrapper = await mountDateTimePicker();
    const newDateString = moment(newTime).add(1, "days").format("YYYY-MM-DD");
    const datePicker = wrapper.findComponent({ name: "date-picker" });
    datePicker.vm.$emit("update:modelValue", newDateString);
    await wrapper.vm.$nextTick();

    expect(moment(wrapper.vm.time).format("YYYY-MM-DD")).toBe(newDateString);
  });
  it("binds v-model, class correctly and sets correct attributes on date-picker and time-picker", async () => {
    const wrapper = await mountDateTimePicker();
    const datePicker = wrapper.findComponent({ name: "date-picker" });
    const timePicker = wrapper.findComponent({ name: "time-picker" });

    expect(datePicker.attributes("modelvalue")).toBe(wrapper.vm.dateString);
    expect(datePicker.attributes("class")).toContain("date-class");
    expect(datePicker.attributes("clear-btn")).toBe("false");
    expect(datePicker.attributes("role")).toBe("combobox");
    expect(timePicker.attributes("role")).toBe("combobox");

    const receivedTime = new Date(
      timePicker.attributes("modelvalue"),
    ).toISOString();

    const expectedTime = new Date(wrapper.vm.time).toISOString();
    expect(receivedTime).toBe(expectedTime);
    expect(timePicker.attributes("class")).toContain("time-class");
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
});
