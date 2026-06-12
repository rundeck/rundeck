import { flushPromises, mount } from "@vue/test-utils";
import OffsetPagination from "../OffsetPagination.vue";

const mountComponent = async (offset = 0, max = 10, total = 30) => {
  const wrapper = mount(OffsetPagination, {
    props: { pagination: { offset, max, total } },
  });
  await flushPromises();
  return wrapper;
};

describe("OffsetPagination", () => {
  it("shows correct active page on mount for different offsets", async () => {
    const wrapper1 = await mountComponent(0, 10, 30);
    expect(
      wrapper1.find('[data-testid="pagination-current-page"]').text(),
    ).toBe("1");

    const wrapper2 = await mountComponent(10, 10, 30);
    expect(
      wrapper2.find('[data-testid="pagination-current-page"]').text(),
    ).toBe("2");

    const wrapper3 = await mountComponent(20, 10, 30);
    expect(
      wrapper3.find('[data-testid="pagination-current-page"]').text(),
    ).toBe("3");
  });

  it("emits the correct offset when user clicks page links", async () => {
    const wrapper = await mountComponent(0, 10, 30);

    await wrapper.find('[data-testid="pagination-page-2"]').trigger("click");
    const changes = wrapper.emitted("change");
    expect(changes).toBeTruthy();
    expect(changes?.[0]).toEqual([10]);

    await wrapper.find('[data-testid="pagination-page-3"]').trigger("click");
    expect(changes?.[1]).toEqual([20]);
  });
});
