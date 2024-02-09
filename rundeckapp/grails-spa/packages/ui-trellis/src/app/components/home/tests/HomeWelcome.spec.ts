import {shallowMount, VueWrapper} from '@vue/test-utils';
import HomeWelcome from '../HomeWelcome.vue';
import messages from '../../../../app/utilities/locales/en_US.js'

const VMarkdownViewStub = {
    name: 'VMarkdownView',
    props: ['content'],
    template: '<div>{{content}}</div>',
}

jest.mock('@/library', () => ({
    getRundeckContext: jest.fn().mockReturnValue({ rdBase: 'http://localhost:4440' }),
}));

const mountHomeWelcome = async (props?: Record<string, any>): Promise<VueWrapper<any>> => {
    return shallowMount(HomeWelcome, {
        props: {
            appTitle: 'Test App',
            buildIdent: '1.0.0',
            logoImage: 'test-logo.png',
            helpLinkUrl: '/help',
            ...props,
        },
        global: {
            mocks: {
                $t: (baseString: string, replacements?: string[]) => {
                    if(!replacements){
                        return messages[baseString]
                    }
                    return messages[baseString].replace(/{(\d+)}/g, (_, index) => replacements[index])
                },
            },
            stubs: {
                VMarkdownView: VMarkdownViewStub,
                FirstRun: {
                    name: 'FirstRun',
                    template: '<div></div>',
                },
            }
        },

    });
};

describe('HomeWelcome', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders with the correct app title, build identifier, logo image, and help link URL', async () => {
        const wrapper = await mountHomeWelcome({
            appTitle: 'Custom App',
            buildIdent: '2.0.0',
            logoImage: 'custom-logo.png',
            helpLinkUrl: '/custom-help',
        });


        expect(wrapper.find('.card-title').text()).toBe('Welcome to Custom App 2.0.0');
        expect(wrapper.find('img').attributes('src')).toBe('/assets/custom-logo.png');
        expect(wrapper.findComponent(VMarkdownViewStub).props('content')).toContain('/custom-help');
    });

    it('displays the correct content in the markdown view', async () => {
        const wrapper = await mountHomeWelcome();


        expect(wrapper.findComponent(VMarkdownViewStub).props('content')).toContain('Test App');
        expect(wrapper.findComponent(VMarkdownViewStub).props('content')).toContain('/help');
    });
});
