import { mount } from "@vue/test-utils";
import ActivityRunningIndicator from "../activityRunningIndicator.vue";

describe("ActivityRunningIndicator.vue", () => {
  let wrapper;
  let mockEventBus;

  beforeEach(() => {
    mockEventBus = {
      on: jest.fn(),
      emit: jest.fn(),
    };

    wrapper = mount(ActivityRunningIndicator, {
      props: {
        eventBus: mockEventBus,
        displayMode: "test",
      },
    });
  });

  it("renders correctly when count is 0", () => {
    expect(wrapper.html()).not.toContain("<span>");
  });

  it("renders correctly when count is greater than 0", async () => {
    wrapper.vm.updateNowrunning(5);
    await wrapper.vm.$nextTick();
    const indicator = wrapper.find('[data-test-id="activity-indicator"]');
    expect(indicator.exists()).toBe(true);
    expect(indicator.text()).toBe("5");
  });

  it("updates count correctly", async () => {
    wrapper.vm.updateNowrunning(10);
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.count).toBe(10);
  });

  it("emits 'activity-nowrunning-click-action' event when clicked", async () => {
    wrapper.vm.updateNowrunning(5);
    await wrapper.vm.$nextTick();
    wrapper.find("span").trigger("click");
    expect(mockEventBus.emit).toHaveBeenCalledWith(
      "activity-nowrunning-click-action",
    );
  });

  it("registers 'activity-nowrunning-count' event on mount", () => {
    expect(mockEventBus.on).toHaveBeenCalledWith(
      "activity-nowrunning-count",
      expect.any(Function),
    );
  });
});
