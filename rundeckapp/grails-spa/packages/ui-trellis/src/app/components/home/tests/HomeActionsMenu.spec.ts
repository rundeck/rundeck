import {mount, VueWrapper} from '@vue/test-utils';
import HomeActionsMenu from '../HomeActionsMenu.vue';
import {Dropdown} from 'uiv';
import {AuthzMeta} from "../types/projectTypes";


const defaultMeta: AuthzMeta  = { name: 'authz', data: { project: {admin: true}, types: { job: { create: true } } } }

const mountHomeActionsMenu = async (): Promise<VueWrapper<any>> => {
    const wrapper = mount(HomeActionsMenu, {
        props: {
            project: {
                name: 'example',
                meta: [defaultMeta],
            },
            index: 0,
        },
        global: {
            mocks: {
                $t: (msg: string) => msg,
            },
        },
        components: {
            Dropdown
        }
    });

    // Wait for the next Vue tick to allow for asynchronous rendering
    await wrapper.vm.$nextTick();

    return wrapper;
};


describe('HomeActionsMenu', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });
    
    it('displays edit configuration link for admin', async () => {
        const wrapper = await mountHomeActionsMenu();

        expect(wrapper.find('[href="/project/example/configure"]').exists()).toBe(true);
    });

    it('does not display edit configuration link for non-admin', async () => {
        defaultMeta.data.project.admin = false;
        const wrapper = await mountHomeActionsMenu();

        expect(wrapper.find('[href="/project/example/configure"]').exists()).toBe(false);
    });

    it('displays create job link for users with create permissions', async () => {
        const wrapper = await mountHomeActionsMenu();

        expect(wrapper.find('[href="/project/example/job/create"]').exists()).toBe(true);
    });

    it('does not display create job link for users without create permissions', async () => {
        defaultMeta.data.types.job.create = false;
        defaultMeta.data.project.admin = false;

        const wrapper = await mountHomeActionsMenu();

        // Assert that the create job link is not displayed
        expect(wrapper.find('[href="/project/example/job/create"]').exists()).toBe(false);
    });
});
