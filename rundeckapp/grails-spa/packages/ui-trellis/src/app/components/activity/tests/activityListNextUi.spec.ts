import {
  i18nMocks,
  mockEventBus,
  mockReports,
  rundeckClientMock,
  rundeckServiceMock,
  mockQueryRunning,
  mockQueryExecutions,
} from "../mocks/mock";
import { flushPromises, shallowMount } from "@vue/test-utils";
import { cloneDeep } from "lodash";
import { Btn, Modal } from "uiv";
import ActivityList from "../activityList.vue";
import ActivityListEaTable from "../ActivityListEaTable.vue";
import OffsetPagination from "../../../../library/components/utils/OffsetPagination.vue";
import { loadJsonData } from "../../../utilities/loadJsonData";

jest.mock("../../../../library/services/executions", () => ({
  queryRunning: mockQueryRunning,
  getExecutions: mockQueryExecutions,
}));
jest.mock("../../../../library/rundeckService", () => rundeckServiceMock);
jest.mock("@rundeck/client", () => rundeckClientMock);
jest.mock("../../../../library/services/api", () => ({
  api: { post: jest.fn(), get: jest.fn(), put: jest.fn() },
}));
jest.mock("../../../utilities/loadJsonData", () => ({
  loadJsonData: jest.fn(),
}));

const mountActivityList = async (uiType: string) => {
  (loadJsonData as jest.Mock).mockReturnValue({ uiType });
  const wrapper = shallowMount(ActivityList, {
    props: { eventBus: mockEventBus, displayMode: "mode" },
    global: {
      components: { OffsetPagination, Modal, Btn },
      stubs: { modal: false, btn: false, "i18n-t": true, ProgressBar: true },
      mocks: { ...i18nMocks },
      directives: { tooltip: () => {} },
    },
  });
  await flushPromises();
  return wrapper;
};

describe("ActivityList next UI table", () => {
  beforeEach(() => {
    mockQueryRunning.mockResolvedValue({
      results: [],
      paging: { max: 20, offset: 0, total: 0, count: 0 },
    });
    mockQueryExecutions.mockResolvedValue({
      results: cloneDeep(mockReports),
      paging: { max: 10, offset: 0, total: 20, count: 2 },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("keeps the legacy history table when the next UI is off", async () => {
    const wrapper = await mountActivityList("current");

    expect(wrapper.findComponent(ActivityListEaTable).exists()).toBe(false);
    expect(wrapper.find(".history-executions").exists()).toBe(true);
    expect(wrapper.findComponent(OffsetPagination).exists()).toBe(true);
  });

  it("renders the shared table with the Activity columns in the next UI", async () => {
    const wrapper = await mountActivityList("next");
    const table = wrapper.findComponent(ActivityListEaTable);

    expect(table.exists()).toBe(true);
    expect(table.props("columns")).toEqual([
      "status",
      "id",
      "dateEnded",
      "nodes",
      "duration",
      "user",
      "title",
      "args",
    ]);
    expect(table.props("showRelativeDate")).toBe(true);
    expect(table.props("legacyRowClasses")).toBe(true);
    expect(table.props("middleClickOpensTab")).toBe(true);
    expect(table.props("executionLinkTarget")).toBe("_self");
    expect(wrapper.find('[data-testid="page-info"]').exists()).toBe(false);
    expect(table.props("showToolbar")).toBe(false);
    expect(table.props("selectable")).toBe(false);
    expect(table.props("reports")).toHaveLength(mockReports.length);
    expect(wrapper.find(".history-executions").exists()).toBe(false);
    expect(wrapper.findComponent(OffsetPagination).exists()).toBe(false);
  });

  it("requests the sorted page from the executions API", async () => {
    const wrapper = await mountActivityList("next");

    await wrapper.findComponent(ActivityListEaTable).vm.$emit("page", {
      offset: 0,
      max: 20,
      sortField: "dateEnded",
      sortOrder: 1,
    });
    await flushPromises();

    expect(mockQueryExecutions).toHaveBeenLastCalledWith(
      expect.anything(),
      expect.objectContaining({
        offset: 0,
        max: 20,
        sortBy: "dateCompleted",
        sortOrder: "ascending",
      }),
    );
  });

  it("feeds the table selection into the existing bulk delete flow", async () => {
    const wrapper = await mountActivityList("next");

    await wrapper
      .find('[data-testid="activity-list-bulk-delete"]')
      .trigger("click");
    const table = wrapper.findComponent(ActivityListEaTable);
    expect(table.props("selectable")).toBe(true);

    await table.vm.$emit("update:selectedIds", [42]);

    expect(
      wrapper.findComponent(ActivityListEaTable).props("selectedIds"),
    ).toEqual([42]);
    expect(
      wrapper
        .find('[data-testid="delete-selected-executions"]')
        .attributes("disabled"),
    ).toBeUndefined();
  });

  it("ignores a superseded history response", async () => {
    const wrapper = await mountActivityList("next");
    let resolveOlder: (value: unknown) => void = () => undefined;
    mockQueryExecutions
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOlder = resolve;
        }),
      )
      .mockResolvedValueOnce({
        results: [{ ...cloneDeep(mockReports[0]), id: 99 }],
        paging: { max: 10, offset: 10, total: 20, count: 1 },
      });
    const table = wrapper.findComponent(ActivityListEaTable);

    await table.vm.$emit("page", { offset: 0, max: 10 });
    await table.vm.$emit("page", { offset: 10, max: 10 });
    await flushPromises();
    resolveOlder({
      results: cloneDeep(mockReports),
      paging: { max: 10, offset: 0, total: 20, count: 2 },
    });
    await flushPromises();

    expect(
      wrapper
        .findComponent(ActivityListEaTable)
        .props("reports")
        .map((report: { id: number }) => report.id),
    ).toEqual([99]);
  });

  it("clears the history error after a successful retry", async () => {
    mockQueryExecutions.mockRejectedValueOnce(new Error("boom"));
    const wrapper = await mountActivityList("next");
    expect(wrapper.find('[data-testid="error-message"]').exists()).toBe(true);

    await wrapper
      .findComponent(ActivityListEaTable)
      .vm.$emit("page", { offset: 0, max: 10 });
    await flushPromises();

    expect(wrapper.find('[data-testid="error-message"]').exists()).toBe(false);
  });
});
