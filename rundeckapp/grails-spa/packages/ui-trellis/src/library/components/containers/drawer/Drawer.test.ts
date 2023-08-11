import { mount } from '@vue/test-utils';
import Drawer from './Drawer.vue';

describe('Drawer', () => {
    test('should render the drawer content when opened', async () => {
        const wrapper = mount(Drawer, { props: { visible: true }});
        await wrapper.vm.$nextTick();

        // Verify that the drawer content is rendered
        expect(wrapper.find('.rd-drawer').exists()).toBe(true);
    });

    test('should emit "close" event when close button is clicked', async () => {
        const wrapper = mount(Drawer, { props: { visible: true }});
        await wrapper.vm.$nextTick();

        // Simulate a click event on the close button
        await wrapper.find('.btn').trigger('click');
        await wrapper.vm.$nextTick();

        // Verify that the "close" event is emitted
        expect(wrapper.emitted('close')).toBeTruthy();
    });

    test('should hide the drawer content when closed', async () => {
        const wrapper = mount(Drawer, { props: { visible: true }});
        await wrapper.vm.$nextTick();

        // Close the drawer
        await wrapper.setProps({ visible: false });
        await wrapper.vm.$nextTick();

        // Verify that the drawer content is hidden
        expect(wrapper.find('.rd-drawer').exists()).toBe(false);
    });
});
