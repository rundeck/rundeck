import { mount } from "@vue/test-utils";
import Column from "primevue/column";
import ActivityListEaTable from "../ActivityListEaTable.vue";
import PtDataTable from "../../../../library/components/primeVue/PtDataTable/PtDataTable.vue";
import PtSelect from "../../../../library/components/primeVue/PtSelect/PtSelect.vue";
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
    expect(
      wrapper.find('[data-testid="activity-ea-data-table"]').text(),
    ).toContain("Showing 1 to 1 of 1 executions");
    expect(wrapper.findComponent(PtDataTable).props()).toMatchObject({
      paginator: true,
      alwaysShowPaginator: true,
      rowsPerPageOptions: [10, 20, 50],
      paginatorTemplate:
        "CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown",
    });
  });

  it("renders every Figma column in the specified order", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper
        .findAll(".p-datatable-thead > tr > th")
        .map((header) => header.text()),
    ).toEqual([
      "Execution",
      "Date/Time",
      "Plugin",
      "Nodes",
      "Duration",
      "Executor",
    ]);
  });

  it("reports the current server-backed result range", async () => {
    const wrapper = await createWrapper({
      props: {
        pagination: { offset: 0, max: 10, total: 30 },
      },
    });

    expect(
      wrapper.find('[data-testid="activity-ea-data-table"]').text(),
    ).toContain("Showing 1 to 10 of 30 executions");
  });

  it("uses the compact Figma duration and plugin presentation", async () => {
    const wrapper = await createWrapper({
      props: {
        reports: [
          {
            ...mockReports[0],
            plugin: {
              title: "Jira / Issue / Comment",
              iconUrl: "/plugin/icon/WorkflowNodeStep/jira-comment",
            },
            dateStarted: { unixtime: 1720195500000 },
            dateEnded: { unixtime: 1720195561552 },
          },
        ],
      },
    });

    expect(wrapper.find(".activity-ea-table__plugin-content").text()).toBe(
      "Jira / Issue / Comment",
    );
    expect(
      wrapper.find('[data-testid="plugin-icon-image"]').attributes("src"),
    ).toBe("/plugin/icon/WorkflowNodeStep/jira-comment");
    expect(
      wrapper
        .find(".activity-ea-table__plugin-content .activity-ea-table__truncate")
        .attributes("title"),
    ).toBe("Jira / Issue / Comment");
    expect(wrapper.find(".fa-puzzle-piece").exists()).toBe(false);
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

  it("keeps the table shell visible during the initial load", async () => {
    const wrapper = await createWrapper({
      props: {
        reports: [],
        pagination: { offset: 0, max: 10, total: 0 },
        loading: true,
      },
    });

    expect(wrapper.find('[data-testid="activity-ea-loading"]').text()).toBe(
      "Loading executions…",
    );
    expect(
      wrapper.find('[data-testid="activity-ea-loading"]').attributes("role"),
    ).toBe("status");
    expect(
      wrapper.find('[data-testid="activity-ea-header-execution"]').text(),
    ).toBe("Execution");
    expect(wrapper.find('[data-testid="activity-ea-empty"]').exists()).toBe(
      false,
    );
  });

  it("replaces existing rows with the inline loader during refresh", async () => {
    const wrapper = await createWrapper({
      props: {
        pagination: { offset: 0, max: 10, total: 30 },
      },
    });

    await wrapper.setProps({ loading: true });
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-testid="activity-ea-loading"]').text()).toBe(
      "Loading executions…",
    );
    expect(
      wrapper.find('[data-testid="activity-ea-execution-42"]').exists(),
    ).toBe(false);

    await wrapper.setProps({ loading: false });
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-testid="activity-ea-loading"]').exists()).toBe(
      false,
    );
    expect(
      wrapper.find('[data-testid="activity-ea-execution-42"]').text(),
    ).toBe("#42");
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
    expect(wrapper.emitted("page")).toEqual([
      [
        {
          offset: 10,
          max: 20,
          sortField: "dateEnded",
          sortOrder: -1,
        },
      ],
    ]);
  });

  it("offers the original relative periods and emits the selected value", async () => {
    const wrapper = await createWrapper({
      props: { recentFilter: "1w" },
    });
    const timeSelect = wrapper.findAllComponents(PtSelect)[0];

    expect(wrapper.find('[data-testid="activity-ea-time-filter"]').text()).toBe(
      "Last week",
    );
    expect(timeSelect.props("options")).toEqual([
      { label: "Any time", value: "" },
      { label: "Last hour", value: "1h" },
      { label: "Last day", value: "1d" },
      { label: "Last week", value: "1w" },
      { label: "Last month", value: "1m" },
    ]);

    await timeSelect.vm.$emit("update:modelValue", "1w");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("update:recentFilter")).toEqual([["1w"]]);
  });

  it("selects and clears a saved Activity filter from the dropdown", async () => {
    const savedFilter = {
      filterName: "My failures",
      query: { statFilter: "fail" },
    };
    const wrapper = await createWrapper({
      props: {
        savedFilters: [savedFilter],
        savedFilterName: "My failures",
      },
    });
    const savedFilterSelect = wrapper.findAllComponents(PtSelect)[1];

    expect(savedFilterSelect.props("options")).toEqual([
      { label: "Saved Filters", value: "" },
      { label: "My failures", value: "My failures" },
    ]);
    expect(
      wrapper.find('[data-testid="activity-ea-saved-filters"]').text(),
    ).toBe("My failures");

    await savedFilterSelect.vm.$emit("update:modelValue", "My failures");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("select-saved-filter")).toEqual([[savedFilter]]);

    await savedFilterSelect.vm.$emit("update:modelValue", "");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("clear-saved-filter")).toEqual([[]]);

    await wrapper.setProps({ savedFilterName: "" });
    await wrapper.vm.$nextTick();

    expect(
      wrapper.find('[data-testid="activity-ea-saved-filters"]').text(),
    ).toBe("Saved Filters");
  });

  it("makes only server-sortable columns sortable", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.findAllComponents(Column).map((column) => ({
        field: column.props("field"),
        sortable: column.props("sortable"),
      })),
    ).toEqual([
      { field: "id", sortable: true },
      { field: "dateEnded", sortable: true },
      { field: "plugin", sortable: false },
      { field: "nodes", sortable: false },
      { field: "duration", sortable: false },
      { field: "user", sortable: true },
    ]);
  });

  it("updates the indicator and requests the first page after sorting", async () => {
    const wrapper = await createWrapper({
      props: {
        pagination: { offset: 20, max: 20, total: 45 },
      },
    });
    const table = wrapper.findComponent(PtDataTable);

    await table.vm.$emit("sort", {
      first: 20,
      rows: 20,
      sortField: "user",
      sortOrder: 1,
    });
    await wrapper.vm.$nextTick();

    expect(table.props("sortField")).toBe("user");
    expect(table.props("sortOrder")).toBe(1);
    expect(wrapper.emitted("page")).toEqual([
      [
        {
          offset: 0,
          max: 20,
          sortField: "user",
          sortOrder: 1,
        },
      ],
    ]);
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

  it("disables select-all while a page is loading", async () => {
    const wrapper = await createWrapper({ props: { loading: true } });

    expect(
      wrapper
        .find('[data-testid="activity-ea-select-all"]')
        .attributes("disabled"),
    ).toBeDefined();
  });

  it("renders only the configured columns and hides the toolbar", async () => {
    const wrapper = await createWrapper({
      props: {
        columns: ["status", "id", "user", "title", "args"],
        showToolbar: false,
      },
    });

    expect(
      wrapper.findAllComponents(Column).map((column) => column.props("field")),
    ).toEqual(["status", "id", "user", "title", "argstring"]);
    expect(
      wrapper.find('[data-testid="activity-ea-execution-count"]').exists(),
    ).toBe(false);
  });

  it("renders columns in the configured order", async () => {
    const wrapper = await createWrapper({
      props: { columns: ["user", "dateEnded", "id"] },
    });

    expect(
      wrapper.findAllComponents(Column).map((column) => column.props("field")),
    ).toEqual(["user", "dateEnded", "id"]);
  });

  it("adds the relative completion time and legacy tooltip when enabled", async () => {
    const wrapper = await createWrapper({
      props: {
        showRelativeDate: true,
        reports: [
          {
            ...mockReports[0],
            dateStarted: {
              unixtime: 1720195500000,
              date: "2024-07-05T16:55:00Z",
            },
            dateEnded: {
              unixtime: 1720195561552,
              date: "2024-07-05T16:56:01Z",
            },
          },
        ],
      },
    });
    const id = mockReports[0].id;

    expect(
      wrapper.find(`[data-testid="activity-ea-date-relative-${id}"]`).text(),
    ).not.toBe("");
    expect(
      wrapper.find(".activity-ea-table__date-content").attributes("title"),
    ).toContain("Completed");
  });

  it("adds legacy row classes and highlights the requested execution", async () => {
    const wrapper = await createWrapper({
      props: {
        legacyRowClasses: true,
        highlightId: mockReports[0].id,
      },
    });
    const row = wrapper.find("tbody tr");

    expect(row.classes()).toEqual(
      expect.arrayContaining([
        "activity_row",
        "autoclickable",
        "succeed",
        "job",
        "highlight",
        `ali-${mockReports[0].id}`,
      ]),
    );
  });

  it("opens the execution in a new tab on a middle-click when enabled", async () => {
    const open = jest.spyOn(window, "open").mockImplementation(() => null);
    const wrapper = await createWrapper({
      props: { middleClickOpensTab: true },
    });

    await wrapper
      .find("tbody tr td:nth-child(2)")
      .trigger("auxclick", { button: 1 });

    expect(open).toHaveBeenCalledWith(mockReports[0].permalink, "_blank");
    open.mockRestore();
  });

  it("includes the configured page size in the rows-per-page options", async () => {
    const wrapper = await createWrapper({
      props: { pagination: { offset: 0, max: 30, total: 1 } },
    });

    expect(
      wrapper.findComponent(PtDataTable).props("rowsPerPageOptions"),
    ).toEqual([10, 20, 30, 50]);
  });

  it("never passes a negative total while the count is unknown", async () => {
    const wrapper = await createWrapper({
      props: { loading: true, pagination: { offset: 0, max: 10, total: -1 } },
    });

    expect(wrapper.findComponent(PtDataTable).props("totalRecords")).toBe(0);
  });

  it("hides the selection checkboxes when not selectable", async () => {
    const wrapper = await createWrapper({ props: { selectable: false } });

    expect(
      wrapper.find('[data-testid="activity-ea-select-all"]').exists(),
    ).toBe(false);
    expect(
      wrapper
        .find(`[data-testid="activity-ea-select-${mockReports[0].id}"]`)
        .exists(),
    ).toBe(false);
  });

  it("disables column sorting when the consumer cannot sort", async () => {
    const wrapper = await createWrapper({ props: { sortable: false } });

    expect(
      wrapper
        .findAllComponents(Column)
        .some((column) => column.props("sortable")),
    ).toBe(false);
  });

  it("shows the job title with referenced, deleted and custom status markers", async () => {
    const wrapper = await createWrapper({
      props: {
        columns: ["status", "title"],
        jobIdFilter: "parent-job",
        reports: [
          {
            ...mockReports[0],
            id: 7,
            status: "needs-review",
            customStatus: "needs-review",
            job: { id: "child-job", name: "deploy", group: "ops" },
          },
          {
            ...mockReports[0],
            id: 8,
            jobDeleted: true,
            description: "echo hi",
          },
        ],
      },
    });

    expect(wrapper.find('[data-testid="activity-ea-title-7"]').text()).toBe(
      "ops/deploy",
    );
    expect(
      wrapper.find('[data-testid="activity-ea-referenced-7"]').text(),
    ).toBe("Referenced");
    expect(
      wrapper.find('[data-testid="activity-ea-custom-status-7"]').text(),
    ).toBe("needs-review");
    expect(
      wrapper
        .find('[data-testid="activity-ea-status-7"]')
        .attributes("data-execstate"),
    ).toBe("OTHER");
    expect(wrapper.find('[data-testid="activity-ea-title-8"]').text()).toBe(
      "echo hi",
    );
    expect(
      wrapper.find('[data-testid="activity-ea-job-deleted-8"]').exists(),
    ).toBe(true);
  });

  it("emits the execution selected by a row click", async () => {
    const wrapper = await createWrapper();
    const table = wrapper.findComponent(PtDataTable);

    await table.vm.$emit("row-click", { data: mockReports[0] });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("row-click");
    expect(wrapper.emitted("row-click")).toEqual([[mockReports[0]]]);
  });

  it("opens the execution link safely without selecting the row", async () => {
    const wrapper = await createWrapper();
    const executionLink = wrapper.find(
      '[data-testid="activity-ea-execution-42"]',
    );

    expect(executionLink.attributes("target")).toBe("_blank");
    expect(executionLink.attributes("rel")).toBe("noopener noreferrer");

    await executionLink.trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("row-click")).toBeUndefined();
  });
});
