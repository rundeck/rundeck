import { mount } from '@vue/test-utils';
import ThemeSelect from './ThemeSelect.vue';

describe('ThemeSelect', () => {
    it('calls a method when an option is selected', async () => {
        const mockMethod = jest.fn(); // Create a mock function to track method calls
        window._rundeck = {
            rootStore: {
                theme: {
                    setUserTheme: mockMethod,
                    userPreferences: {
                        theme: 'system'
                    }
                }
            }
        }
        const wrapper = mount(ThemeSelect);

        // Simulate selecting an option
        const optionIndex = 1;
        const option = wrapper.find(`option:nth-child(${optionIndex + 1})`);
        await option.setValue(true);

        // Assert that the method was called with the correct option
        expect(mockMethod).toHaveBeenCalledWith('light');
    });
});
