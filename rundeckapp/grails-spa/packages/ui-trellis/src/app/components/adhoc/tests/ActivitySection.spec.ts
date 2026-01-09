import { mount } from "@vue/test-utils";
import ActivitySection from "../ActivitySection.vue";
import ActivityList from "../../activity/activityList.vue";

jest.mock("../../../../library/services/api", () => ({
  apiClient: jest.fn(() => ({
    get: jest.fn(),
    post: jest.fn(),
  })),
}));

jest.mock("../../../../library", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    apiVersion: "44",
    projectName: "test-project",
    rdBase: "http://localhost:4440",
    data: {
      jobslistDateFormatMoment: "YYYY-MM-DD",
      runningDateFormatMoment: "YYYY-MM-DD HH:mm:ss",
    },
  }),
}));

jest.mock("vue-i18n", () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}));

describe("ActivitySection", () => {
  const mockEventBus = {
    on: jest.fn(),
    emit: jest.fn(),
    off: jest.fn(),
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

