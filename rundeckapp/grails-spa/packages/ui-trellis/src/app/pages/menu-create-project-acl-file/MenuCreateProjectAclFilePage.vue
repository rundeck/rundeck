<template>
  <div data-testid="acl-create-page">
    <div class="acl-create-layout">
      <div class="container-fluid">
        <div class="row">
          <div class="col-xs-12">
            <acl-file-editor-form
              :project="pageData.project"
              :name="pageData.name"
              :file-text="pageData.fileText"
              :field-errors="pageData.fieldErrors"
              :validation="pageData.validation"
              :save-url="pageData.saveUrl"
              :back-url="pageData.backUrl"
              :csrf="pageData.csrf"
              :help-links="pageData.helpLinks"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";

import AclFileEditorForm from "../../components/menu-create-project-acl-file/AclFileEditorForm.vue";
import type { AclCreatePageData } from "../../components/menu-create-project-acl-file/types/aclFileTypes";

export default defineComponent({
  name: "MenuCreateProjectAclFilePage",
  components: {
    AclFileEditorForm,
  },
  props: {
    pageData: {
      type: Object as PropType<AclCreatePageData>,
      required: true,
    },
  },
});
</script>

<style scoped lang="scss">
.acl-create-layout {
  width: var(--sizes-full);
}
</style>
