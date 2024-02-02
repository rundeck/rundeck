import { shallowMount } from '@vue/test-utils';
import HomeView from '../HomeView.vue';
import { getProjects, getProjectNames } from '../services/homeServices';

jest.mock('@/app/components/home/services/homeServices');
jest.mock('@/library', () => ({
    getRundeckContext: jest.fn().mockReturnValue({ rdBase: 'http://localhost:4440' }),
}));

const mockedGetProjects = getProjects as jest.MockedFunction<typeof getProjects>;
const mockedGetProjectNames = getProjectNames as jest.MockedFunction<typeof getProjectNames>;

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
        console.warn = jest.fn();
    });

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

    it('calls getProjects and getProjectNames on component mount', async () => {
        const getProjectsSpy = jest.spyOn(HomeView.methods, 'getProjects');
        const getPartialDataSpy = jest.spyOn(HomeView.methods, 'getPartialData');

        createWrapper();

        expect(getProjectsSpy).toHaveBeenCalled();
        expect(getPartialDataSpy).toHaveBeenCalled();

        getProjectsSpy.mockRestore();
        getPartialDataSpy.mockRestore();
    });

    it('correctly sets projects and projectCount when getProjects is successful', async () => {
        const projectsData = [{ name: 'Project1' }, { name: 'Project2' }];
        mockedGetProjects.mockResolvedValueOnce(projectsData);
        const wrapper = createWrapper();

        await wrapper.vm.$nextTick();

        expect(wrapper.vm.projects).toEqual(projectsData);
        expect(wrapper.vm.projectCount).toBe(projectsData.length);
    });

    it('correctly sets projectCount when getPartialData is successful', async () => {
        const projectNamesData = ['Project1', 'Project2'];
        mockedGetProjectNames.mockResolvedValueOnce(projectNamesData);
        const wrapper = createWrapper();

        await wrapper.vm.$nextTick();

        expect(wrapper.vm.projectCount).toBe(projectNamesData.length);
    });
});
