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

    it("updates value when the user types and emits 'update:modelValue'", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        await input.setValue("new value");
        await input.trigger("input");
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted("update:modelValue")).toBeTruthy();
        expect(wrapper.emitted("update:modelValue")[0]).toEqual(["new value"]);
    });
    it("emits 'onChange' when the input changes", async () => {
        const wrapper = await createWrapper();
        const autoComplete = wrapper.findComponent(AutoComplete);
        await autoComplete.vm.$emit("change", { value: "new value" });
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted("onChange")).toBeTruthy();
        expect(wrapper.emitted("onChange")[0]).toEqual([{ value: "new value" }]);
    });
    it("emits 'onComplete' when a suggestion is selected", async () => {
        const wrapper = await createWrapper();
        const input = wrapper.find("input.p-inputtext");
        await input.setValue("Option 1");
        await input.trigger("input");
        const autoComplete = wrapper.findComponent(AutoComplete);
        await autoComplete.vm.$emit("complete", [{ label: "Option 1", value: 1 }]);
        await wrapper.vm.$nextTick();
        expect(wrapper.emitted("onComplete")).toBeTruthy();
        expect(wrapper.emitted("onComplete")[0]).toEqual([
            [{ label: "Option 1", value: 1 }],
        ]);
    });
    it("renders the correct placeholder text and empty search message", async () => {
        const wrapper = await createWrapper({ placeholder: "Search here.." });
        const input = wrapper.find("input.p-inputtext");
        expect(input.attributes("placeholder")).toBe("Search here..");
    });
});