import { mount } from "@vue/test-utils";

import AclValidationReport from "../AclValidationReport.vue";

const createWrapper = async (errors: Record<string, string[]> = {}) => {
  const wrapper = mount(AclValidationReport, {
    props: {
      validation: {
        valid: false,
        errors,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("AclValidationReport", () => {
  it("renders validation documents and messages in sorted order", async () => {
    const wrapper = await createWrapper({
      "acls/z-policy": ["Second document error"],
      "acls/a-policy": ["First document error"],
    });

    expect(
      wrapper
        .findAll('[data-testid="acl-validation-document-name"]')
        .map((element) => element.text()),
    ).toEqual(["acls/a-policy", "acls/z-policy"]);
    expect(
      wrapper
        .findAll('[data-testid="acl-validation-message"]')
        .map((element) => element.text()),
    ).toEqual(["First document error", "Second document error"]);
  });

  it("renders no entries when validation has no errors", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.findAll('[data-testid="acl-validation-document"]'),
    ).toHaveLength(0);
  });
});
