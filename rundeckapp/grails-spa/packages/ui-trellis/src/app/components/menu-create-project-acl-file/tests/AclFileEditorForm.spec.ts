import { shallowMount } from "@vue/test-utils";

import AceEditor from "@/library/components/utils/AceEditor.vue";
import AclFileEditorForm from "../AclFileEditorForm.vue";

const createWrapper = async (props: Record<string, unknown> = {}) => {
  const wrapper = shallowMount(AclFileEditorForm, {
    props: {
      project: "test",
      name: null,
      fileText: "",
      fieldErrors: {},
      validation: null,
      saveUrl: "/project/test/admin/acls/save",
      backUrl: "/project/test/admin/acls",
      csrf: {
        TOKEN: "csrf-token",
        URI: "/project/test/admin/acls/create",
      },
      helpLinks: {
        accessControlPolicy:
          "https://docs.rundeck.com/docs/administration/security/authorization.html",
        aclPolicyFormat:
          "https://docs.rundeck.com/docs/administration/security/authorization.html#example",
      },
      ...props,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("AclFileEditorForm", () => {
  it("renders the project, form destination, and CSRF fields", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.find('[data-testid="acl-project-value"]').text()).toBe(
      "test",
    );
    expect(
      wrapper.find('[data-testid="acl-file-form"]').attributes("action"),
    ).toBe("/project/test/admin/acls/save");
    expect(
      wrapper.find('[data-testid="acl-csrf-token"]').attributes("value"),
    ).toBe("csrf-token");
    expect(
      wrapper.find('[data-testid="acl-csrf-uri"]').attributes("value"),
    ).toBe("/project/test/admin/acls/create");
    expect(
      wrapper.find('[data-testid="acl-name-input"]').attributes("autocomplete"),
    ).toBe("off");
    expect(
      wrapper.find('[data-testid="acl-file-text-input"]').attributes("id"),
    ).toBe("acl-file-text-input");
  });

  it("opens the help menu with both documentation links", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(false);
    await wrapper.find('[data-testid="acl-help-toggle"]').trigger("click");
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(true);
    expect(
      wrapper
        .find('[data-testid="acl-help-access-control-link"]')
        .attributes("href"),
    ).toBe(
      "https://docs.rundeck.com/docs/administration/security/authorization.html",
    );
    expect(
      wrapper.find('[data-testid="acl-help-format-link"]').attributes("href"),
    ).toBe(
      "https://docs.rundeck.com/docs/administration/security/authorization.html#example",
    );
    expect(
      wrapper.find('[data-testid="acl-help-format-link"]').attributes("target"),
    ).toBe("_new");
  });

  it("shows name, policy, and validation errors", async () => {
    const wrapper = await createWrapper({
      name: "existing-policy",
      fileText: "invalid: yaml",
      fieldErrors: {
        name: ["ACL Policy already exists: existing-policy"],
        fileText: ["Policy cannot be blank"],
      },
      validation: {
        valid: false,
        errors: {
          "acls/existing-policy": ["The policy is invalid"],
        },
      },
    });

    expect(
      (
        wrapper.find('[data-testid="acl-name-input"]')
          .element as HTMLInputElement
      ).value,
    ).toBe("existing-policy");
    expect(wrapper.find('[data-testid="acl-name-error"]').text()).toBe(
      "ACL Policy already exists: existing-policy",
    );
    expect(wrapper.find('[data-testid="acl-file-text-error"]').text()).toBe(
      "Policy cannot be blank",
    );
    expect(
      wrapper.find('[data-testid="acl-validation-section"]').text(),
    ).toContain("error.validation.failed.title");
  });

  it("arms leave confirmation after editing and disarms it before save", async () => {
    const wrapper = await createWrapper();
    const firstEvent = new Event("beforeunload", { cancelable: true });
    const firstPreventDefault = jest.spyOn(firstEvent, "preventDefault");

    await wrapper
      .findComponent(AceEditor)
      .vm.$emit("update:modelValue", "description: changed");
    await wrapper.vm.$nextTick();
    window.dispatchEvent(firstEvent);

    expect(firstPreventDefault).toHaveBeenCalledTimes(1);

    await wrapper.find('[data-testid="acl-save-button"]').trigger("click");
    await wrapper.vm.$nextTick();
    const secondEvent = new Event("beforeunload", { cancelable: true });
    const secondPreventDefault = jest.spyOn(secondEvent, "preventDefault");
    window.dispatchEvent(secondEvent);

    expect(secondPreventDefault).not.toHaveBeenCalled();
  });

  it("preserves the entered policy name when the editor updates", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="acl-name-input"]')
      .setValue("valid-policy");
    await wrapper
      .findComponent(AceEditor)
      .vm.$emit("update:modelValue", "description: valid policy");
    await wrapper.vm.$nextTick();

    expect(
      (
        wrapper.find('[data-testid="acl-name-input"]')
          .element as HTMLInputElement
      ).value,
    ).toBe("valid-policy");
    expect(
      wrapper.find('[data-testid="acl-name-input"]').attributes("name"),
    ).toBe("name");
    expect(
      new FormData(
        wrapper.find('[data-testid="acl-file-form"]')
          .element as HTMLFormElement,
      ).get("name"),
    ).toBe("valid-policy");
  });

  it("identifies and focuses the generated ACE input", async () => {
    const wrapper = await createWrapper();
    const editorInput = document.createElement("textarea");
    const focus = jest.fn();

    await wrapper.findComponent(AceEditor).vm.$emit("init", {
      focus,
      textInput: {
        getElement: () => editorInput,
      },
    });
    await wrapper.vm.$nextTick();

    expect(focus).toHaveBeenCalledTimes(1);
    expect(editorInput.id).toBe("acl-policy-editor-input");
  });

  it("does not show field or validation errors for an empty form", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.find('[data-testid="acl-name-error"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="acl-file-text-error"]').exists()).toBe(
      false,
    );
    expect(
      wrapper.find('[data-testid="acl-validation-section"]').exists(),
    ).toBe(false);
  });
});
