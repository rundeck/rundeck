import { vi } from "vitest";
import { mount, flushPromises } from "@vue/test-utils";

vi.mock("@/library", () => ({
  getRundeckContext: vi.fn(() => ({
    projectName: "testProject",
    rootStore: {
      ui: { addItems: vi.fn() },
    },
  })),
}));

vi.mock("@/library/stores/NodeFilterLocalstore", () => ({
  NodeFilterStore: vi.fn().mockImplementation(() => ({
    selectedFilter: "",
    setSelectedFilter: vi.fn(),
  })),
}));

vi.mock("@/app/utilities/uiSocketObserver", () => ({
  observer: { observe: vi.fn() },
}));

vi.mock("@/app/components/job/resources/NodeFilterInput.vue", () => ({
  default: { name: "NodeFilterInput", template: "<div />" },
}));

vi.mock("@/app/components/job/resources/NodeCard.vue", () => ({
  default: { name: "NodeCard", template: "<div />" },
}));

import { FilterInputComp } from "./main";

function makeKoFilter(filterVal = "") {
  const filterObservable = vi.fn().mockReturnValue(filterVal) as any;
  filterObservable.subscribe = vi.fn()
    .mockReturnValue({ dispose: vi.fn() });
  return {
    filter: filterObservable,
    selectNodeFilter: vi.fn(),
  };
}

const mountComp = (filter: string) =>
  mount(FilterInputComp, {
    props: {
      itemData: { filter },
      extraAttrs: {},
    },
    global: {
      stubs: { NodeFilterInput: { template: "<div />" } },
    },
  });

describe("FilterInputComp attachKnockout", () => {
  afterEach(() => {
    delete (window as any).nodeFilter;
  });

  it("pushes preset filterValue into KO when filterValue is initialized from itemData", async () => {
    const koFilter = makeKoFilter("ko-default");
    (window as any).nodeFilter = koFilter;

    const wrapper = mountComp("name: preset-node");
    await flushPromises();

    expect(koFilter.selectNodeFilter).toHaveBeenCalledWith(
      { filter: "name: preset-node" },
      false,
    );
    expect(wrapper.vm.filterValue).toBe("name: preset-node");
  });

  it("reads filterValue from KO when itemData has no filter", async () => {
    const koFilter = makeKoFilter("name: from-ko");
    (window as any).nodeFilter = koFilter;

    const wrapper = mountComp("");
    await flushPromises();

    expect(koFilter.selectNodeFilter).not.toHaveBeenCalled();
    expect(wrapper.vm.filterValue).toBe("name: from-ko");
  });

  it("retries attachKnockout when KO is not yet available", async () => {
    vi.useFakeTimers();
    const koFilter = makeKoFilter("retry-filter");

    const wrapper = mountComp("");

    expect(wrapper.vm.filterValue).toBe("");
    expect(koFilter.selectNodeFilter).not.toHaveBeenCalled();

    (window as any).nodeFilter = koFilter;
    vi.advanceTimersByTime(1000);
    await flushPromises();

    expect(wrapper.vm.filterValue).toBe("retry-filter");

    vi.useRealTimers();
  });
});
