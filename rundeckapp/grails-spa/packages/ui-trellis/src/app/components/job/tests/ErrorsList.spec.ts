import { mount, VueWrapper } from '@vue/test-utils';
import ErrorsList from '../options/ErrorsList.vue'

// Helper function to mount the component
const mountErrorsList = async (options:{errors:any}): Promise<VueWrapper<any>> => {
  const wrapper = mount(ErrorsList, {
    props: {
      errors:options.errors,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};



describe('ErrorsList', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders a single string', async () => {
    const wrapper = await mountErrorsList({errors: 'Error message'});

    // Assert that project details are rendered correctly
    expect(wrapper.text()).toBe('Error message');
  });
  it('renders a single array value', async () => {
    const wrapper = await mountErrorsList({errors: ['Error message']});

    // Assert that project details are rendered correctly
    expect(wrapper.text()).toBe('Error message');
  });
  it('renders array as list', async () => {
    const wrapper = await mountErrorsList({errors: ['Error message1','Error message2']});

    // Assert that project details are rendered correctly
    expect(wrapper.find('ul').exists()).toBeTruthy();
    expect(wrapper.findAll('li').length).toBe(2);
    expect(wrapper.findAll('li').map(e=>e.text())).toStrictEqual(['Error message1','Error message2']);
  });
});
