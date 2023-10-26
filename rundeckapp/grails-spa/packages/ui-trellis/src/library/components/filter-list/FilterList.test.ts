import { mount } from '@vue/test-utils';
import FilterList from './FilterList.vue';
import {Webhook} from "../../stores/Webhooks";

describe('FilterList', () => {
    const items = [
        { id: 'apple', name: 'Apple' },
        { id: 'banana', name: 'Banana' },
        { id: 'orange', name: 'Orange' },
        { id: 'mango', name: 'Mango' },
    ] as Webhook[];

    it('renders the filtered items based on the search query', async () => {
        const wrapper = mount(FilterList, {
            props: { items },
            slots: {
                item: '<div class="item">{{ params.item.name }}</div>',
            },
        });

        // Find the input element
        const input = wrapper.find('input');

        // Set the search query
        await input.setValue('an');

        // Find the rendered items
        const renderedItems = wrapper.findAll('.item');

        // Assert that the filtered items are rendered correctly
        expect(renderedItems).toHaveLength(3);
        expect(renderedItems[0].text()).toBe('Banana');
        expect(renderedItems[1].text()).toBe('Orange');
        expect(renderedItems[2].text()).toBe('Mango');
    });

    it('renders no items when there is no match for the search query', async () => {
        const wrapper = mount(FilterList, {
            props: { items },
            slots: {
                item: '<div class="item">{{ params.item.name }}</div>',
            },
        });

        // Find the input element
        const input = wrapper.find('input');

        // Set the search query
        await input.setValue('xyz');

        // Find the rendered items
        const renderedItems = wrapper.findAll('.item');

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
        const input = wrapper.find('input');

        // Set the search query
        await input.setValue('a');

        // Find the first rendered item
        const firstItem = wrapper.find('.scroller__item');

        // Trigger a click on the item
        await firstItem.trigger('click');

        // Assert that the "item:selected" event is emitted with the correct payload
        expect(wrapper.emitted('item:selected')).toBeTruthy();
        expect(wrapper.emitted('item:selected')[0]).toEqual([{ id: 'apple', name: 'Apple' }]);
    });
});
