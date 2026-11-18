import { mount } from "@vue/test-utils";
import DataTable from "primevue/datatable";
import PtDataTable from "../PtDataTable.vue";

interface MountOptions {
  props?: Record<string, unknown>;
  slots?: Record<string, string>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(PtDataTable, {
    props: {
      value: [{ id: "42" }],
      dataKey: "id",
      ...options.props,
    },
    slots: options.slots,
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("PtDataTable", () => {
  it("passes data and lazy pagination settings to PrimeVue", async () => {
    const wrapper = await createWrapper({
      props: {
        first: 10,
        rows: 10,
        totalRecords: 30,
        lazy: true,
        paginator: true,
      },
    });

    const dataTable = wrapper.findComponent(DataTable);
    expect(dataTable.props("value")).toEqual([{ id: "42" }]);
    expect(dataTable.props("first")).toBe(10);
    expect(dataTable.props("totalRecords")).toBe(30);
    expect(dataTable.props("lazy")).toBe(true);
  });

  it("renders exposed content slots", async () => {
    const wrapper = await createWrapper({
      props: { value: [] },
      slots: {
        header: '<span data-testid="pt-data-table-header">Activity</span>',
        empty: '<span data-testid="pt-data-table-empty">No executions</span>',
      },
    });

    expect(wrapper.find('[data-testid="pt-data-table-header"]').text()).toBe(
      "Activity",
    );
    expect(wrapper.find('[data-testid="pt-data-table-empty"]').text()).toBe(
      "No executions",
    );
  });

  it("forwards page events with their payload", async () => {
    const wrapper = await createWrapper();
    const dataTable = wrapper.findComponent(DataTable);

    await dataTable.vm.$emit("page", {
      first: 10,
      rows: 10,
      page: 1,
      pageCount: 3,
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("page");
    expect(wrapper.emitted("page")).toEqual([
      [{ first: 10, rows: 10, page: 1, pageCount: 3 }],
    ]);
  });

  it("passes sort state and forwards sort events", async () => {
    const wrapper = await createWrapper({
      props: {
        sortField: "user",
        sortOrder: -1,
      },
    });
    const dataTable = wrapper.findComponent(DataTable);

    expect(dataTable.props("sortField")).toBe("user");
    expect(dataTable.props("sortOrder")).toBe(-1);

    await dataTable.vm.$emit("sort", {
      first: 0,
      rows: 10,
      sortField: "user",
      sortOrder: 1,
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("sort")).toEqual([
      [
        {
          first: 0,
          rows: 10,
          sortField: "user",
          sortOrder: 1,
        },
      ],
    ]);
  });

  it("forwards selection updates with their payload", async () => {
    const wrapper = await createWrapper();
    const dataTable = wrapper.findComponent(DataTable);

    await dataTable.vm.$emit("update:selection", [{ id: "42" }]);
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty("update:selection");
    expect(wrapper.emitted("update:selection")).toEqual([[[{ id: "42" }]]]);
  });
});
