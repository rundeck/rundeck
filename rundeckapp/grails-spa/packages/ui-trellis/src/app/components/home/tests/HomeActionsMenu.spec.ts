import {mount, VueWrapper} from '@vue/test-utils';
import HomeActionsMenu from '../HomeActionsMenu.vue';

interface ComponentProps {
    project: {
        name: string;
        meta: Array<{ name: string; data: any }>;
    };
    index: number;
}


describe('HomeActionsMenu', () => {
    const mountHomeActionsMenu = (props?: ComponentProps): VueWrapper<any> => {
        const defaultProps: ComponentProps = {
            project: {
                name: 'example',
                meta: [{ name: 'authz', data: { project: {}, types: { job: { create: true } } } }],
            },
            index: 0,
        }

        return mount(HomeActionsMenu, {
            ...defaultProps,
            props,
        });
    };

    it('renders the component correctly', () => {
        const wrapper = mountHomeActionsMenu();

        expect(wrapper.exists()).toBe(true);
    });

    it('displays edit configuration link for admin', async () => {
        const wrapper = mount(HomeActionsMenu, {
            props: {
                project: {
                    name: 'example',
                    meta: [{ name: 'authz', data: { project: { admin: true }, types: { job: { create: true } } } }],
                },
                index: 0,
            },
        });

        // Assert that the edit configuration link is displayed
        expect(wrapper.find('[href="/project/example/configure"]').exists()).toBe(true);
    });

    it('does not display edit configuration link for non-admin', async () => {
        const wrapper = mount(HomeActionsMenu, {
            props: {
                project: {
                    name: 'example',
                    meta: [{ name: 'authz', data: { project: { admin: false }, types: { job: { create: true } } } }],
                },
                index: 0,
            },
        });

        // Assert that the edit configuration link is not displayed
        expect(wrapper.find('[href="/project/example/configure"]').exists()).toBe(false);
    });

    it('displays create job link for users with create permissions', async () => {
        const wrapper = mount(HomeActionsMenu, {
            props: {
                project: {
                    name: 'example',
                    meta: [{ name: 'authz', data: { project: { admin: true }, types: { job: { create: true } } } }],
                },
                index: 0,
            },
        });

        // Assert that the create job link is displayed
        expect(wrapper.find('[href="/project/example/job/create"]').exists()).toBe(true);
    });

    it('does not display create job link for users without create permissions', async () => {
        const wrapper = mount(HomeActionsMenu, {
            props: {
                project: {
                    name: 'example',
                    meta: [{ name: 'authz', data: { project: { admin: true }, types: { job: { create: false } } } }],
                },
                index: 0,
            },
        });

        // Assert that the create job link is not displayed
        expect(wrapper.find('[href="/project/example/job/create"]').exists()).toBe(false);
    });

    // Add more tests as needed for other scenarios
});
