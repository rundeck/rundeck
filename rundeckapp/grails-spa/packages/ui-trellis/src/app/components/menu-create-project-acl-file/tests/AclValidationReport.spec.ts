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

  it("shows the source identity help icon beside each document", async () => {
    const wrapper = await createWrapper({
      "acls/a-policy": ["First document error"],
      "acls/b-policy": ["Second document error"],
    });

    const helpIcons = wrapper.findAll(
      '[data-testid="acl-validation-identity-help"]',
    );
    expect(helpIcons).toHaveLength(2);
    expect(helpIcons[0].attributes("title")).toBe(
      "acl.validation.error.sourceIdentity.help",
    );
    expect(helpIcons[0].classes()).toEqual(
      expect.arrayContaining(["has_tooltip", "text-info"]),
    );
    expect(helpIcons[0].find(".glyphicon-question-sign").exists()).toBe(true);
  });

  it("renders every message when a document repeats the same message", async () => {
    const wrapper = await createWrapper({
      "acls/a-policy": ["Duplicate error", "Duplicate error"],
    });

    expect(
      wrapper
        .findAll('[data-testid="acl-validation-message"]')
        .map((element) => element.text()),
    ).toEqual(["Duplicate error", "Duplicate error"]);
  });

  it("renders no entries when validation has no errors", async () => {
    const wrapper = await createWrapper();

    expect(
      wrapper.findAll('[data-testid="acl-validation-document"]'),
    ).toHaveLength(0);
  });
});
