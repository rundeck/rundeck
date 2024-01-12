import { shallowMount } from '@vue/test-utils';
import HomeView from '../HomeView.vue';
import { getProjects } from '../services/homeServices';

jest.mock('@/app/components/home/services/homeServices');

const mockedGetProjects = getProjects as jest.MockedFunction<typeof getProjects>;

const createWrapper = (props = {}) => {
    return shallowMount(HomeView, {
        props: {
            appTitle: 'Test App',
            buildIdent: 'Test Build',
            logoImage: 'test-logo.png',
            helpLinkUrl: 'https://example.com',
            ...props,
        },
        global: {
            mocks: {
                $t: jest.fn().mockImplementation((msg) => msg),
                $tc: jest.fn().mockImplementation((msg) => msg),
            },
        },
    });
};

describe('HomeView', () => {
    beforeEach(() => {
        console.warn = jest.fn()
        console.warn('you cant see me')
    })

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders HomeHeader component when data is loaded and projectCount > 0', async () => {
        mockedGetProjects.mockResolvedValueOnce([]);
        const wrapper = createWrapper();
        await wrapper.setData({ dataLoaded: true, projectCount: 1 });

        expect(wrapper.findComponent({ name: 'HomeHeader' }).exists()).toBe(true);
    });

    it('renders FirstRun component when data is loaded, projectCount is 0, and createProjectAllowed is true', async () => {
        mockedGetProjects.mockResolvedValueOnce([]);
        const wrapper = createWrapper({ createProjectAllowed: true });
        await wrapper.setData({ dataLoaded: true, projectCount: 0 });

        expect(wrapper.findComponent({ name: 'FirstRun' }).exists()).toBe(true);
    });

    it('renders warning message when data is loaded, projectCount is 0, and createProjectAllowed is false', async () => {
        mockedGetProjects.mockResolvedValueOnce([]);
        const wrapper = createWrapper();
        await wrapper.setData({ dataLoaded: true, projectCount: 0 });
        await wrapper.vm.$nextTick();

        expect(wrapper.find('.text-warning').exists()).toBe(true);
    });

    it('renders HomeCardList component when data is loaded, projectCount is greater than 0, and createProjectAllowed is false', async () => {
        mockedGetProjects.mockResolvedValueOnce([]);
        const wrapper = createWrapper();
        await wrapper.setData({ dataLoaded: true, projectCount: 1 });

        expect(wrapper.findComponent({ name: 'HomeCardList' }).exists()).toBe(true);
    });
});
