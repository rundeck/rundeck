import { mount } from '@vue/test-utils';
import Tabs from './Tabs.vue';
import Tab from './Tab.vue';

describe('Tabs', () => {
    test('should render tab titles correctly', () => {
        const wrapper = mount(Tabs, {
            slots: {
                default: `
                    <Tab :index="0" title="Tab 1" ></Tab>
                    <Tab :index="1" title="Tab 2" ></Tab>
                    <Tab :index="2" title="Tab 3" ></Tab>
                `,
            },
            global: {
                components: {
                    Tab,
                },
            },
        });

        const tabTitles = wrapper.findAll('.patab-item');

        // Verify the number of tab titles rendered
        expect(tabTitles.length).toBe(3);

        // Verify the text content of each tab title
        expect(tabTitles[0].text()).toBe('Tab 1');
        expect(tabTitles[1].text()).toBe('Tab 2');
        expect(tabTitles[2].text()).toBe('Tab 3');
    });

    test('should display content of selected tab', async () => {
        const wrapper = mount(Tabs, {
            slots: {
                default: `
                    <Tab :index="0" title="Tab 1">Content 1</Tab>
                    <Tab :index="1" title="Tab 2">Content 2</Tab>
                    <Tab :index="2" title="Tab 3">Content 3</Tab>
                `,
            },
            global: {
                components: {
                    Tab,
                },
            },
        });

        const tabTitles = wrapper.findAll('.patab-item');

        // Simulate clicking on the second tab
        await tabTitles[1].trigger('click');

        const tabContent = wrapper.find('.patab-content');

        // Verify that the content of the second tab is displayed
        expect(tabContent.text()).toBe('Content 2');
    });
});
