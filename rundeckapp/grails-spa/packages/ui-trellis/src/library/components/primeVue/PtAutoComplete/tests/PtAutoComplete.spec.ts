import { mount } from "@vue/test-utils";
import PtAutoComplete from "../PtAutoComplete.vue";
const createWrapper = async (props = {}): Promise<any> => {
    const wrapper = mount(PtAutoComplete, {
        props: {
            modelValue: "initial value",
            suggestions: [{ label: "Option 1", value: 1 }, { label: "Option 2", value: 2 }],
            defaultValue: "default value",
            name: "test-autocomplete",
            optionLabel: "label",
            ...props,
        },
        global: {
            stubs: {
                AutoComplete: {
                    template: `
                        <div>
                            <input
                                class='p-inputtext'
                                @input='$emit("update:modelValue", $event.target.value)'
                                @change='$emit("onChange", $event)'
                                @complete='$emit("complete", "test suggestion")'
                            />
                        </div>
                    `,
                },
            },
        },
    });
    await wrapper.vm.$nextTick();
    return wrapper;
};
describe("PtAutoComplete", () => {
    it("renders the component and passes props correctly", async () => {
        const wrapper = await createWrapper();
        expect(wrapper.props("modelValue")).toBe("initial value");
        expect(wrapper.props("suggestions")).toEqual([
            { label: "Option 1", value: 1 },
            { label: "Option 2", value: 2 },
        ]);
        expect(wrapper.props("name")).toBe("test-autocomplete");
        expect(wrapper.props("optionLabel")).toBe("label");
    });
    it("updates value when the user types and emits 'update:modelValue'", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        // Simulate user input
        await input.setValue("new value");
        // Check if the correct event is emitted
        expect(wrapper.emitted("update:modelValue")).toBeTruthy();
        expect(wrapper.emitted("update:modelValue")[0]).toEqual(["new value"]);
    });
    it("emits 'onChange' when the input changes", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        // Simulate change event
        await input.trigger("change");
        // Check if the 'onChange' event is emitted
        expect(wrapper.emitted("onChange")).toBeTruthy();
        expect(wrapper.emitted("onChange")[0]).toBeDefined();

    });
    it("emits 'onComplete' when a suggestion is selected", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        // Simulate the 'complete' event
        await input.trigger("complete");
        // Check if the 'onComplete' event is emitted with the correct payload
        expect(wrapper.emitted("onComplete")).toBeTruthy();
        expect(wrapper.emitted("onComplete")[0]).toEqual(["test suggestion"]);
    });
});