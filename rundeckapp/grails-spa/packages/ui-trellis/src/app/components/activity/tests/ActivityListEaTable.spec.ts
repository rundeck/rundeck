import { mount } from "@vue/test-utils";
import ActivityListEaTable from "../ActivityListEaTable.vue";
import PtDataTable from "../../../../library/components/primeVue/PtDataTable/PtDataTable.vue";
import { i18nMocks, mockReports } from "../mocks/mock";

interface MountOptions {
  props?: Record<string, unknown>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(ActivityListEaTable, {
    props: {
      reports: [mockReports[0]],
      pagination: {
        offset: 0,
        max: 10,
        total: 1,
      },
      loading: false,
      bulkEditMode: false,
      selectedIds: [],
      ...options.props,
    },
    global: {
      mocks: i18nMocks,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("ActivityListEaTable", () => {
  it("renders the EA execution columns and report", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.find('[data-testid="activity-ea-execution-count"]').text(),
    ).toBe("1 Execution");
    expect(wrapper.find('[data-testid="activity-ea-time-filter"]').text()).toBe(
      "Any time",
    );
    expect(
      wrapper.find('[data-testid="activity-ea-saved-filters"]').text(),
    ).toBe("Saved Filters");
    expect(
      wrapper.find('[data-testid="activity-ea-header-execution"]').text(),
    ).toBe("Execution");
    expect(
      wrapper.find('[data-testid="activity-ea-header-date-time"]').text(),
    ).toBe("Date/Time");
    expect(
      wrapper.find('[data-testid="activity-ea-header-plugin"]').text(),
    ).toBe("Plugin");
    expect(
      wrapper.find('[data-testid="activity-ea-header-nodes"]').text(),
    ).toBe("Nodes");
    expect(
      wrapper.find('[data-testid="activity-ea-header-duration"]').text(),
    ).toBe("Duration");
    expect(
      wrapper.find('[data-testid="activity-ea-header-executor"]').text(),
    ).toBe("Executor");
    expect(
      wrapper.find('[data-testid="activity-ea-execution-42"]').text(),
    ).toBe("#42");
    expect(wrapper.find('[data-testid="activity-ea-select-42"]').exists()).toBe(
      true,
    );
  });

  it("uses the compact Figma duration and plugin presentation", async () => {
    const wrapper = await createWrapper({
      props: {
        reports: [
          {
            ...mockReports[0],
            job: { name: "Jira / Issue / Comment" },
            dateStarted: { unixtime: 1720195500000 },
            dateEnded: { unixtime: 1720195561552 },
          },
        ],
      },
    });

    expect(wrapper.find(".activity-ea-table__plugin-content").text()).toBe(
      "Jira / Issue / Comment",
    );
    expect(wrapper.find(".activity-ea-table__plugin-icon").classes()).toContain(
      "fa-jira",
    );
    expect(wrapper.find('[data-testid="activity-ea-duration-42"]').text()).toBe(
      "1m 1s",
    );
  });

  it("renders the empty state when there are no reports", async () => {
    const wrapper = await createWrapper({
      props: {
        reports: [],
        pagination: { offset: 0, max: 10, total: 0 },
      },
    });

    expect(wrapper.find('[data-testid="activity-ea-empty"]').text()).toBe(
      "No results for the query",
    );
  });

  it("emits the requested offset and page size", async () => {
    const wrapper = await createWrapper();
    const table = wrapper.findComponent(PtDataTable);

    await table.vm.$emit("page", {
      first: 10,
      rows: 20,
      page: 1,
      pageCount: 2,
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("page");
    expect(wrapper.emitted("page")).toEqual([[{ offset: 10, max: 20 }]]);
  });

  it("emits selected execution IDs from the always-visible row checkbox", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="activity-ea-select-42"]')
      .trigger("change");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("update:selectedIds");
    expect(wrapper.emitted("update:selectedIds")).toEqual([[[42]]]);
  });

  it("selects every execution shown on the page", async () => {
    const wrapper = await createWrapper({
      props: {
        reports: mockReports,
        pagination: { offset: 0, max: 10, total: 2 },
      },
    });

    await wrapper
      .find('[data-testid="activity-ea-select-all"]')
      .trigger("change");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:selectedIds")).toEqual([[[42, 43]]]);
  });

  it("emits the execution selected by a row click", async () => {
    const wrapper = await createWrapper();
    const table = wrapper.findComponent(PtDataTable);

    await table.vm.$emit("row-click", { data: mockReports[0] });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("row-click");
    expect(wrapper.emitted("row-click")).toEqual([[mockReports[0]]]);
  });
});
