import { mount, VueWrapper } from '@vue/test-utils';
import HomeHeader from '../HomeHeader.vue';
import { getAppLinks } from '../../../../library';
import { getSummary } from '../services/homeServices';

jest.mock('../../../../library');
jest.mock('src/app/components/home/services/homeServices');

const mockGetAppLinks = getAppLinks as jest.Mock;
const mockGetSummary = getSummary as jest.Mock;

const mockSummaryResponse = {
    execCount: 10,
    totalFailedCount: 2,
    recentProjects: ['Project1', 'Project2'],
    recentUsers: ['User1', 'User2'],
};

mockGetAppLinks.mockReturnValue({
    frameworkCreateProject: '/createProject',
});

mockGetSummary.mockResolvedValue(mockSummaryResponse);

const mountHomeHeader = async (props?: Record<string, any>): Promise<VueWrapper<any>> => {
    return mount(HomeHeader, {
        props: {
            createProjectAllowed: false,
            projectCount: 5,
            ...props,
        },
        global: {
            mocks: {},
        },
    });
};

describe('HomeHeader', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the component correctly', async () => {
        const wrapper = await mountHomeHeader();

        expect(wrapper.exists()).toBe(true);
    });

    it('displays project count when loaded', async () => {
        const wrapper = await mountHomeHeader();

        expect(wrapper.find('#projectCountNumber').text()).toBe('5');
        expect(wrapper.find('.text-h3').text()).toBe('5 projects');
    });

    it('displays loading spinner when project count is not loaded', async () => {
        const wrapper = await mountHomeHeader({ projectCount: 0 });

        expect(wrapper.find('.loading-spinner').exists()).toBe(true);
    });

    it('displays create project button when allowed', async () => {
        const wrapper = await mountHomeHeader({ createProjectAllowed: true });

        expect(wrapper.find('#createProject').exists()).toBe(true);
    });

    it('does not display create project button when not allowed', async () => {
        const wrapper = await mountHomeHeader({ createProjectAllowed: false });

        expect(wrapper.find('#createProject').exists()).toBe(false);
    });

    it('displays execution summary when summary is loaded', async () => {
        const wrapper = await mountHomeHeader();

        expect(wrapper.find('.summary-count.text-info').text()).toBe('10');
        expect(wrapper.find('.summary-count.text-warning').text()).toBe('2');
    });

    it('displays recent projects and recent users when available', async () => {
        const wrapper = await mountHomeHeader();

        expect(wrapper.find('.project-link').exists()).toBe(true);
        expect(wrapper.find('.project-link').text()).toBe('Project1');
        expect(wrapper.find('.summary-count.text-info').text()).toBe('2');
        expect(wrapper.find('.text-info').text()).toBe('User1, User2');
    });

    it('displays placeholder when project count is not loaded', async () => {
        mockGetSummary.mockResolvedValueOnce(null);
        const wrapper = await mountHomeHeader({ projectCount: 0 });

        expect(wrapper.find('.text-muted').text()).toBe('...');
    });
});
