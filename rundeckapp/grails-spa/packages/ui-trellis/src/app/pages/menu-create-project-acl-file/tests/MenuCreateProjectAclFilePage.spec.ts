import { mount } from "@vue/test-utils";

import AclFileEditorForm from "../../../components/menu-create-project-acl-file/AclFileEditorForm.vue";
import MenuCreateProjectAclFilePage from "../MenuCreateProjectAclFilePage.vue";

const createWrapper = async () => {
  const wrapper = mount(MenuCreateProjectAclFilePage, {
    props: {
      pageData: {
        project: "test",
        name: null,
        fileText: "",
        create: true,
        fieldErrors: {},
        validation: null,
        conflict: false,
        saveUrl: "/project/test/admin/acls/save",
        cancelUrl: "/project/test/admin/acls",
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
      },
    },
    global: {
      stubs: {
        AclFileEditorForm: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("MenuCreateProjectAclFilePage", () => {
  it("passes server data to the editor form", async () => {
    const wrapper = await createWrapper();

    expect(wrapper.findComponent(AclFileEditorForm).props("project")).toBe(
      "test",
    );
    expect(wrapper.findComponent(AclFileEditorForm).props("saveUrl")).toBe(
      "/project/test/admin/acls/save",
    );
  });
});
