import { flushPromises } from "@vue/test-utils";
import { nextTick } from "vue";

jest.mock("../../../utilities/i18n", () => ({
  initI18n: () => ({
    install: (app: {
      config: { globalProperties: Record<string, unknown> };
    }) => {
      app.config.globalProperties.$t = (key: string) => key;
    },
  }),
}));

import { mountDynamicForms } from "../main";

const propertyName = "executionLifecyclePlugins.key.configMap.userRoiData";

const createLegacyHost = () => {
  const form = document.createElement("form");
  const externalInput = document.createElement("input");
  externalInput.id = "legacy-dynamic-form-input";
  externalInput.name = propertyName;
  externalInput.type = "hidden";
  externalInput.dataset.testid = "legacy-dynamic-form-input";

  const host = document.createElement("div");
  host.className = "dynamic-form-vue";
  host.setAttribute(
    "fields",
    JSON.stringify({
      field1: {
        key: "field1",
        label: "Option1",
        value: "field1",
        desc: "Description1",
      },
    }),
  );
  host.setAttribute("options", "");
  host.setAttribute("hasOptions", "false");
  host.setAttribute("element", externalInput.id);
  host.setAttribute("name", propertyName);
  form.append(externalInput, host);
  document.body.append(form);

  return { form, externalInput };
};

const findByTestId = (testId: string) =>
  document.querySelector<HTMLInputElement>(`[data-testid="${testId}"]`);

describe("dynamic-form legacy bootstrap", () => {
  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("forwards edited fields to the legacy input used for form submission", async () => {
    const { form, externalInput } = createLegacyHost();
    mountDynamicForms();
    await flushPromises();

    const fieldInput = findByTestId("field-input-0");
    if (!fieldInput) {
      throw new Error("Expected the dynamic form field input to render");
    }
    fieldInput.value = "Updated Value";
    fieldInput.dispatchEvent(new Event("input", { bubbles: true }));
    await nextTick();
    fieldInput.dispatchEvent(new Event("change", { bubbles: true }));
    await flushPromises();

    expect(externalInput.value).toBe(
      '[{"key":"field1","label":"Option1","value":"Updated Value","desc":"Description1"}]',
    );
    expect(new FormData(form).getAll(propertyName)).toEqual([
      '[{"key":"field1","label":"Option1","value":"Updated Value","desc":"Description1"}]',
    ]);
  });
});
