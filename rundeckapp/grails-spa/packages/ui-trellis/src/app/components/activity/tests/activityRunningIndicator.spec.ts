import { mount } from "@vue/test-utils";
import ActivityRunningIndicator from "../activityRunningIndicator.vue";
import mitt, { Emitter, EventType } from "mitt";

const mountActivityRunningIndicator = async (props = {}) => {
  const mockEventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
    all: new Map(),
  };

  return mount(ActivityRunningIndicator, {
    props: {
      eventBus: mockEventBus,
      displayMode: "test",
      ...props,
    },
  });
};

describe("ActivityRunningIndicator.vue", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly when count is 0", async () => {
    const wrapper = await mountActivityRunningIndicator();
    expect(wrapper.html()).not.toContain("<span>");
  });

  it("renders correctly when count is greater than 0", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const wrapper = await mountActivityRunningIndicator({
      eventBus: eventBus,
    });
    eventBus.emit("activity-nowrunning-count", 5);
    await wrapper.vm.$nextTick();
    const indicator = wrapper.find('[data-test-id="activity-indicator"]');
    expect(indicator.exists()).toBe(true);
    expect(indicator.text()).toBe("5");
  });

  it("emits the right event and updates the count correctly", async () => {
    const eventBus: Emitter<Record<EventType, any>> = mitt();
    const emitSpy = jest.spyOn(eventBus, "emit");
    const wrapper = await mountActivityRunningIndicator({
      eventBus: eventBus,
    });
    eventBus.emit("activity-nowrunning-count", 10);
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).count).toBe(10);
    wrapper.find("span").trigger("click");
    expect(emitSpy).toHaveBeenCalledWith("activity-nowrunning-click-action");
  });
  it("registers 'activity-nowrunning-count' event on mount", async () => {
    const wrapper = await mountActivityRunningIndicator();
    expect(wrapper.vm.eventBus.on).toHaveBeenCalledWith(
      "activity-nowrunning-count",
      expect.any(Function),
    );
  });
  it("unregisters 'activity-nowrunning-count' event on unmount", async () => {
    const wrapper = await mountActivityRunningIndicator();
    wrapper.unmount();
    expect(wrapper.vm.eventBus.off).toHaveBeenCalledWith(
      "activity-nowrunning-count",
      expect.any(Function),
    );
  });
});
