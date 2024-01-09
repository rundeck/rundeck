import { mount } from '@vue/test-utils';
import CopyBox from './CopyBox.vue';

describe('CopyBox', () => {
    test('should copy text to clipboard when clicked', async () => {
        const wrapper = mount(CopyBox, { props: { content: "text" }});

        // Mock the "execCommand" method of document to simulate copying
        document.execCommand = jest.fn();

        // Simulate a click event on the component
        await wrapper.trigger('click');

        // Verify that the "execCommand" method was called with the expected argument
        expect(document.execCommand).toHaveBeenCalledWith('copy');
    });

    test('should display success message after successful copy', async () => {
        const wrapper = mount(CopyBox, { props: { content: "text" }});

        // Mock the "execCommand" method to return true, indicating successful copy
        document.execCommand = jest.fn().mockReturnValue(true);

        // Simulate a click event on the component
        await wrapper.trigger('click');
        await wrapper.vm.$nextTick();

        // Verify that the success message is displayed
        expect(wrapper.text()).toContain('Copied to clipboard!');
    });

    test('should display error message after failed copy', async () => {
        const wrapper = mount(CopyBox, { props: { content: "text" }});

        // Mock the "execCommand" method to return false, indicating failed copy
        document.execCommand = jest.fn().mockReturnValue(false);

        // Simulate a click event on the component
        await wrapper.trigger('click');
        await wrapper.vm.$nextTick();

        // Verify that the error message is displayed
        expect(wrapper.text()).toContain('Copy failed!');
    });
});