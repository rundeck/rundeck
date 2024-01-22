import { shallowMount } from '@vue/test-utils';
import HomeCardList from '../HomeCardList.vue';
import HomeSearchBar from "../HomeSearchBar.vue";


jest.mock('@/library/rundeckService.ts', () => ({
    getRundeckContext: jest.fn().mockImplementation(() => ({ eventBus: { on: jest.fn() } })),
}));

const createWrapper = (props = {}) => {
    return shallowMount(HomeCardList, {
        props: {
            loadedProjectNames: false,
            projects: [],
            ...props,
        },
        global: {
            mocks: {
                $t: (msg: string): string => msg,
                $tc: (msg: string): string => msg
            },
        }
    });
};

describe('HomeCardList', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders HomeSearchBar and table header when no projects are available', async () => {
        const wrapper = createWrapper({
            loadedProjectNames: true
        });
        await wrapper.vm.$nextTick();

        expect(wrapper.findComponent(HomeSearchBar).exists()).toBe(true);
        expect(wrapper.find('.project_list_header').exists()).toBe(true);
    });

    it('renders correct alert info message based on searchedProjectsCount', async () => {
        const wrapper = createWrapper({
            loadedProjectNames: true, projects: [{ name: 'Project1' }, { name: 'Project2' }]
        });

        await wrapper.setData({ search: 'gibberish'});
        wrapper.vm.handleSearch();
        await wrapper.vm.$nextTick();

        const searchResultsElement = wrapper.find('[data-test="searchResults"]');
        expect(searchResultsElement.exists()).toBe(true);
        expect(wrapper.vm.searchResultsCount).toBe(0);

        const spanElement = searchResultsElement.find('span');
        expect(spanElement.classes('text-warning')).toBe(true);


        await wrapper.setData({ search: 'Project' });
        await wrapper.vm.$nextTick();

        wrapper.vm.handleSearch();
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.searchResultsCount).toBe(2);
        expect(spanElement.classes('text-white')).toBe(true);
    });

    it('handles search by label and updates filteredProjects and showSearchResults', async () => {
        const wrapper = createWrapper({
            projects: [
                { name: 'Project1', label: 'Label1' },
                { name: 'Project2', label: 'Label2' },
                { name: 'Project3', label: 'Label3' },
            ],
        });

        await wrapper.setData({ search: 'Label' });
        await wrapper.vm.handleSearch();

        expect(wrapper.vm.filteredProjects).toHaveLength(3);
        expect(wrapper.vm.showSearchResults).toBe(true);
    });

    it('hides search results on focus and blur', async () => {
        const wrapper = createWrapper({
            projects: [
                { name: 'Project1', label: 'Label1' },
                { name: 'Project2', label: 'Label2' },
                { name: 'Project3', label: 'Label3' },
            ],
        });

        await wrapper.setData({ search: 'Label' });
        await wrapper.vm.handleSearch();

        expect(wrapper.vm.showSearchResults).toBe(true);

        await wrapper.vm.hideResults();
        expect(wrapper.vm.showSearchResults).toBe(false);
    });
});
