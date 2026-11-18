import { shallowMount } from "@vue/test-utils";

import AceEditor from "@/library/components/utils/AceEditor.vue";
import AclFileEditorForm from "../AclFileEditorForm.vue";

let mountedWrapper: ReturnType<typeof shallowMount> | null = null;

const createWrapper = async (props: Record<string, unknown> = {}) => {
  const wrapper = shallowMount(AclFileEditorForm, {
    attachTo: document.body,
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
  mountedWrapper = wrapper;
  await wrapper.vm.$nextTick();
  return wrapper;
};

const openHelpMenu = async (
  wrapper: Awaited<ReturnType<typeof createWrapper>>,
) => {
  await wrapper.find('[data-testid="acl-help-toggle"]').trigger("click");
  expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(true);
};

const editPolicy = async (
  wrapper: Awaited<ReturnType<typeof createWrapper>>,
  value: string,
) => {
  await wrapper.findComponent(AceEditor).vm.$emit("update:modelValue", value);
  await wrapper.vm.$nextTick();
};

const leavePagePrevented = (): boolean => {
  const event = new Event("beforeunload", { cancelable: true });
  const preventDefault = jest.spyOn(event, "preventDefault");
  window.dispatchEvent(event);
  return preventDefault.mock.calls.length > 0;
};

describe("AclFileEditorForm", () => {
  afterEach(() => {
    mountedWrapper?.unmount();
    mountedWrapper = null;
  });

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

  it("gives the hidden policy mirror field an accessible label", async () => {
    const wrapper = await createWrapper();

    const mirrorField = wrapper.find('[data-testid="acl-file-text-input"]');
    expect(mirrorField.attributes("aria-label")).toBeTruthy();
    expect(mirrorField.attributes("aria-hidden")).toBe("true");
    expect(mirrorField.attributes("tabindex")).toBe("-1");
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

  it("closes the help menu when the user clicks outside it", async () => {
    const wrapper = await createWrapper();
    await openHelpMenu(wrapper);

    await wrapper.find('[data-testid="acl-project-value"]').trigger("click");

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(false);
  });

  it("closes the help menu and refocuses the toggle on Escape", async () => {
    const wrapper = await createWrapper();
    await openHelpMenu(wrapper);

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }));
    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(false);
    expect(document.activeElement).toBe(
      wrapper.find('[data-testid="acl-help-toggle"]').element,
    );
  });

  it("closes the help menu when a help link is chosen", async () => {
    const wrapper = await createWrapper();
    await openHelpMenu(wrapper);

    await wrapper.find('[data-testid="acl-help-format-link"]').trigger("click");

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(false);
  });

  it("keeps the help menu open when the user clicks inside the dropdown", async () => {
    const wrapper = await createWrapper();
    await openHelpMenu(wrapper);

    await wrapper.find('[data-testid="acl-help-dropdown"]').trigger("click");

    expect(wrapper.find('[data-testid="acl-help-menu"]').exists()).toBe(true);
  });

  it("renders every field error even when messages repeat", async () => {
    const wrapper = await createWrapper({
      fieldErrors: {
        name: ["Duplicate name error", "Duplicate name error"],
        fileText: ["Duplicate policy error", "Duplicate policy error"],
      },
    });

    expect(wrapper.findAll('[data-testid="acl-name-error"]')).toHaveLength(2);
    expect(wrapper.findAll('[data-testid="acl-file-text-error"]')).toHaveLength(
      2,
    );
  });

  it("keeps leave confirmation armed after the policy is reverted", async () => {
    const wrapper = await createWrapper({ fileText: "description: original" });

    await editPolicy(wrapper, "description: changed");
    await editPolicy(wrapper, "description: original");

    expect(leavePagePrevented()).toBe(true);
  });

  it("disarms leave confirmation when the user cancels", async () => {
    const wrapper = await createWrapper();

    await editPolicy(wrapper, "description: changed");
    await wrapper.find('[data-testid="acl-cancel-button"]').trigger("click");

    expect(leavePagePrevented()).toBe(false);
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

  it("arms leave confirmation when the user types in the Name field", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="acl-name-input"]')
      .trigger("keydown", { key: "a" });

    expect(leavePagePrevented()).toBe(true);
  });

  it("arms leave confirmation when the Name field value changes", async () => {
    const wrapper = await createWrapper();

    await wrapper.find('[data-testid="acl-name-input"]').setValue("my-policy");

    expect(leavePagePrevented()).toBe(true);
  });

  it("arms leave confirmation when an editor control such as Soft Wrap changes", async () => {
    const wrapper = await createWrapper();

    await wrapper.findComponent(AceEditor).trigger("change");

    expect(leavePagePrevented()).toBe(true);
  });

  it("disarms leave confirmation on save after typing a name", async () => {
    const wrapper = await createWrapper();

    await wrapper.find('[data-testid="acl-name-input"]').setValue("my-policy");
    await wrapper.find('[data-testid="acl-save-button"]').trigger("click");

    expect(leavePagePrevented()).toBe(false);
  });

  it("does not ask for leave confirmation before the user interacts", async () => {
    await createWrapper();

    expect(leavePagePrevented()).toBe(false);
  });

  it("labels the Name and Project fields with label elements like the legacy page", async () => {
    const wrapper = await createWrapper();

    const nameLabel = wrapper.find('[data-testid="acl-name-label"]');
    expect(nameLabel.element.tagName).toBe("LABEL");
    expect(nameLabel.attributes("for")).toBe(
      wrapper.find('[data-testid="acl-name-input"]').attributes("id"),
    );
    expect(nameLabel.classes()).toContain("control-label");
    expect(
      wrapper.find('[data-testid="acl-project-label"]').element.tagName,
    ).toBe("LABEL");
  });

  it("shows the legacy arrow icon in the Back link", async () => {
    const wrapper = await createWrapper();

    const icon = wrapper.find('[data-testid="acl-back-icon"]');
    expect(icon.classes()).toEqual(
      expect.arrayContaining(["glyphicon", "glyphicon-arrow-left"]),
    );
    expect(wrapper.find('[data-testid="acl-back-link"]').text()).toBe(
      "navigation.back.title",
    );
  });

  it("shows an accessible tooltip warning icon when validation fails", async () => {
    const wrapper = await createWrapper({
      validation: {
        valid: false,
        errors: { "acls/policy": ["The policy is invalid"] },
      },
    });

    const icon = wrapper.find('[data-testid="acl-validation-warning-icon"]');
    expect(icon.classes()).toEqual(
      expect.arrayContaining(["glyphicon-warning-sign", "has_tooltip"]),
    );
    expect(icon.attributes("title")).toBe("aclpolicy.format.validation.failed");
    expect(icon.attributes("aria-label")).toBe(
      "aclpolicy.format.validation.failed",
    );
    expect(icon.attributes("aria-hidden")).toBeUndefined();
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
