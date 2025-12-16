import { mount } from "@vue/test-utils";
import { setActivePinia, createPinia } from "pinia";
import FacetFilters from "../FacetFilters.vue";
import { useRepositoriesStore } from "../../stores/repositories.store";

describe("FacetFilters.vue", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it("renders correctly", () => {
    const wrapper = mount(FacetFilters);
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.text()).toContain("Support");
    expect(wrapper.text()).toContain("Community");
    expect(wrapper.text()).toContain("Rundeck Supported");
    expect(wrapper.text()).toContain("Enterprise Exclusive");
  });

  it("calls setSupportTypeFilter when supportType changes", async () => {
    const store = useRepositoriesStore();
    const setSupportTypeFilterSpy = jest.spyOn(
      store,
      "setSupportTypeFilter",
    );

    const wrapper = mount(FacetFilters);
    const checkboxes = wrapper.findAll('input[type="checkbox"]');

    await checkboxes[0].setValue(true);
    await wrapper.vm.$nextTick();

    expect(setSupportTypeFilterSpy).toHaveBeenCalled();
  });

  it("watches supportType changes", async () => {
    const store = useRepositoriesStore();
    const setSupportTypeFilterSpy = jest.spyOn(
      store,
      "setSupportTypeFilter",
    );

    const wrapper = mount(FacetFilters);
    wrapper.setData({ supportType: ["Community"] });
    await wrapper.vm.$nextTick();

    expect(setSupportTypeFilterSpy).toHaveBeenCalledWith(["Community"]);
  });
});

