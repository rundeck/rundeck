import { mount } from "@vue/test-utils";
import Drawer from "./Drawer.vue";

const createWrapper = async (props = {}) => {
  const wrapper = mount(Drawer, { props: { visible: true, ...props } });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("Drawer", () => {
  test("should render the drawer content when opened", async () => {
    const wrapper = await createWrapper();
    expect(wrapper.find('[data-testid="drawer-panel"]').exists()).toBe(true);
  });

  test('should emit "close" event when close button is clicked', async () => {
    const wrapper = await createWrapper();
    await wrapper.find('[data-testid="drawer-close-button"]').trigger("click");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("close")).toBeTruthy();
  });

  test("should hide the drawer content when closed", async () => {
    const wrapper = await createWrapper();
    await wrapper.setProps({ visible: false });
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-testid="drawer-panel"]').exists()).toBe(false);
  });

  test("should only render the mask close control while open with a localized label", async () => {
    const closedWrapper = await createWrapper({ visible: false });
    expect(
      closedWrapper.find('[data-testid="drawer-mask-close"]').exists(),
    ).toBe(false);

    const openWrapper = await createWrapper();
    const mask = openWrapper.find('[data-testid="drawer-mask-close"]');
    expect(mask.exists()).toBe(true);
    expect(mask.attributes("aria-label")).toBe("message_close");

    await mask.trigger("keydown.enter");
    await openWrapper.vm.$nextTick();
    await mask.trigger("keydown.space");
    await openWrapper.vm.$nextTick();

    expect(openWrapper.emitted("close")).toHaveLength(2);
  });
});
