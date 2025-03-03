import { mount, config } from "@vue/test-utils";
import PtAutoComplete from "../PtAutoComplete.vue";
import AutoComplete from "primevue/autocomplete";

config.global.mocks = {
    $primevue: {
        config: {}
    }
};


const createWrapper = async (props = {}): Promise<any> => {
    const wrapper = mount(PtAutoComplete, {
        props: {
            modelValue: "initial value",
            suggestions: [
                { label: "Option 1", value: 1 },
                { label: "Option 2", value: 2 },
            ],
            name: "test-autocomplete",
            placeholder: "Type here...",
            emptySearchMessage: "No results found",
            ...props,
        },
        global: {
            components: {
                AutoComplete: AutoComplete,
            },
        },
    });
    await wrapper.vm.$nextTick();
    return wrapper;
};
describe("PtAutoComplete", () => {
    it("renders the component and passes props correctly", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        expect(input.element.value).toBe("initial value");
        await wrapper.vm.$nextTick();
        const autoComplete = wrapper.findComponent(AutoComplete);
        expect(autoComplete.props("name")).toBe("test-autocomplete");
    });
});