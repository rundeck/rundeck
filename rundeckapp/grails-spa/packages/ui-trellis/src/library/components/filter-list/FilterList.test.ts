import { mount, DOMWrapper } from "@vue/test-utils";
import FilterList from "./FilterList.vue";
import { Webhook } from "../../stores/Webhooks";

function sortByVisualPosition(elements: DOMWrapper<Element>[]) {
  const offsetOf = (el: DOMWrapper<Element>) => {
    const view = el.element.closest(
      ".vue-recycle-scroller__item-view",
    ) as HTMLElement | null;
    const match = view?.style.transform.match(
      /translateY\((-?\d+(\.\d+)?)px\)/,
    );
    return match ? parseFloat(match[1]) : 0;
  };
  return [...elements].sort((a, b) => offsetOf(a) - offsetOf(b));
}

describe("FilterList", () => {
  const items = [
    { id: "apple", name: "Apple" },
    { id: "banana", name: "Banana" },
    { id: "orange", name: "Orange" },
    { id: "mango", name: "Mango" },
  ] as Webhook[];

  it("renders the filtered items based on the search query", async () => {
    const wrapper = mount(FilterList, {
      props: { items },
      slots: {
        item: '<div class="item">{{ params.item.name }}</div>',
      },
    });

    // Find the input element
    const input = wrapper.find("input");

    // Set the search query
    await input.setValue("an");

    // Find the rendered items, in visual (not DOM insertion) order
    const renderedItems = sortByVisualPosition(wrapper.findAll(".item"));

    // Assert that the filtered items are rendered correctly
    expect(renderedItems).toHaveLength(3);
    expect(renderedItems[0].text()).toBe("Banana");
    expect(renderedItems[1].text()).toBe("Orange");
    expect(renderedItems[2].text()).toBe("Mango");
  });

  it("renders no items when there is no match for the search query", async () => {
    const wrapper = mount(FilterList, {
      props: { items },
      slots: {
        item: '<div class="item">{{ params.item.name }}</div>',
      },
    });

    // Find the input element
    const input = wrapper.find("input");

    // Set the search query
    await input.setValue("xyz");

    // Find the rendered items
    const renderedItems = wrapper.findAll(".item");

    // Assert that no items are rendered
    expect(renderedItems).toHaveLength(0);
  });
  it('emits "item:selected" event when an item is clicked', async () => {
    const wrapper = mount(FilterList, {
      props: { items },
      slots: {
        item: '<div class="item">{{ params.item.name }}</div>',
      },
    });

    // Find the input element
    const input = wrapper.find("input");

    // Set the search query
    await input.setValue("a");

    // Find the first rendered item, in visual (not DOM insertion) order
    const firstItem = sortByVisualPosition(
      wrapper.findAll(".scroller__item"),
    )[0];

    // Trigger a click on the item
    await firstItem.trigger("click");

    // Assert that the "item:selected" event is emitted with the correct payload
    expect(wrapper.emitted("item:selected")).toBeTruthy();
    expect(wrapper.emitted("item:selected")![0]).toEqual([
      { id: "apple", name: "Apple" },
    ]);
  });
});
