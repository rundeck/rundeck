import { vi } from "vitest";
import { mount } from "@vue/test-utils";
import ActivitySection from "../ActivitySection.vue";
import ActivityList from "../../activity/activityList.vue";

vi.mock("../../../../library/services/api", () => ({
  apiClient: vi.fn(() => ({
    get: vi.fn(),
    post: vi.fn(),
  })),
}));

vi.mock("../../../../library", () => ({
  getRundeckContext: vi.fn().mockReturnValue({
    apiVersion: "44",
    projectName: "test-project",
    rdBase: "http://localhost:4440",
    data: {
      jobslistDateFormatMoment: "YYYY-MM-DD",
      runningDateFormatMoment: "YYYY-MM-DD HH:mm:ss",
    },
  }),
}));

describe("ActivitySection", () => {
  const mockEventBus = {
    on: vi.fn(),
    emit: vi.fn(),
    off: vi.fn(),
  };

  it("should render correctly", () => {
    const wrapper = mount(ActivitySection, {
      props: {
        eventBus: mockEventBus,
      },
    });

    expect(wrapper.find(".card").exists()).toBe(true);
    expect(wrapper.find(".card-title").exists()).toBe(true);
  });

  it("should pass eventBus to activity-list", () => {
    const wrapper = mount(ActivitySection, {
      props: {
        eventBus: mockEventBus,
      },
    });

    const activityList = wrapper.findComponent(ActivityList);
    expect(activityList.exists()).toBe(true);
    expect(activityList.props("eventBus")).toStrictEqual(mockEventBus);
  });
});
