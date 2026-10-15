<template>
  <form
    class="form-horizontal"
    method="post"
    :action="saveUrl"
    data-testid="acl-file-form"
    @submit="handleSubmit"
    @change="markDirty"
    @keydown="markDirty"
  >
    <input type="hidden" name="project" :value="project" />
    <input type="hidden" name="create" value="true" />
    <input
      type="hidden"
      name="SYNCHRONIZER_TOKEN"
      :value="csrf.TOKEN"
      data-testid="acl-csrf-token"
    />
    <input
      type="hidden"
      name="SYNCHRONIZER_URI"
      :value="csrf.URI"
      data-testid="acl-csrf-uri"
    />

    <div class="card" data-testid="acl-file-card">
      <div class="card-header">
        <h3 class="card-title acl-card-title">
          <a
            :href="backUrl"
            class="btn btn-xs btn-simple"
            :title="$t('navigation.back.title')"
            data-testid="acl-back-link"
          >
            <i
              class="glyphicon glyphicon-arrow-left"
              aria-hidden="true"
              data-testid="acl-back-icon"
            ></i>
            <span class="sr-only">{{ $t("navigation.back.title") }}</span>
          </a>
          {{ $t("create.project.acl.file") }}
          <div class="buttons pull-right">
            <div
              ref="helpDropdown"
              class="dropdown"
              :class="{ open: helpOpen }"
              data-testid="acl-help-dropdown"
            >
              <button
                id="aclHelp"
                ref="helpToggle"
                type="button"
                class="btn btn-default btn-sm dropdown-toggle"
                :aria-expanded="helpOpen"
                data-testid="acl-help-toggle"
                @click="toggleHelp"
              >
                {{ $t("acl.create.help") }}
                <span class="caret" aria-hidden="true"></span>
              </button>
              <ul
                v-if="helpOpen"
                class="dropdown-menu dropdown-menu-right"
                aria-labelledby="aclHelp"
                data-testid="acl-help-menu"
              >
                <li>
                  <a
                    :href="helpLinks.accessControlPolicy"
                    target="_new"
                    data-testid="acl-help-access-control-link"
                    @click="closeHelp"
                  >
                    {{ $t("link.title.access.control.policy") }}
                    <i class="fas fa-arrow-right" aria-hidden="true"></i>
                  </a>
                </li>
                <li>
                  <a
                    :href="helpLinks.aclPolicyFormat"
                    target="_new"
                    data-testid="acl-help-format-link"
                    @click="closeHelp"
                  >
                    {{ $t("link.title.acl.policy.format") }}
                    <i class="fas fa-arrow-right" aria-hidden="true"></i>
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </h3>
      </div>

      <div class="card-content">
        <div
          class="form-group"
          :class="{ 'has-error': nameErrors.length > 0 }"
          data-testid="acl-name-group"
        >
          <label
            for="acl-policy-name"
            class="control-label col-sm-2"
            data-testid="acl-name-label"
          >
            {{ $t("policy.name.label.prompt") }}
          </label>
          <div class="col-sm-10">
            <input
              id="acl-policy-name"
              v-model="localName"
              class="form-control"
              type="text"
              name="name"
              autocomplete="off"
              data-testid="acl-name-input"
              @keypress.enter.prevent
            />
            <div class="help-block">
              {{ $t("policy.name.description") }}
            </div>
            <div
              v-for="(error, index) in nameErrors"
              :key="`name-error-${index}`"
              class="help-block"
              data-testid="acl-name-error"
            >
              {{ error }}
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="control-label col-sm-2" data-testid="acl-project-label">
            {{ $t("project.label.prompt") }}
          </label>
          <div class="col-sm-10">
            <p
              class="form-control-static text-info"
              data-testid="acl-project-value"
            >
              {{ project }}
            </p>
          </div>
        </div>

        <div class="card">
          <div class="card-content">
            <div
              class="form-group"
              :class="{ 'has-error': fileTextErrors.length > 0 }"
              data-testid="acl-file-text-group"
            >
              <div class="col-sm-12 acl-editor-container">
                <ace-editor
                  v-model="localFileText"
                  identifier="acl-policy-editor"
                  lang="yaml"
                  height="var(--acl-editor-height)"
                  :soft-wrap-control="true"
                  data-testid="acl-editor"
                  @init="focusEditor"
                  @update:model-value="markDirty"
                />
                <textarea
                  id="acl-file-text-input"
                  class="sr-only"
                  name="fileText"
                  :value="localFileText"
                  aria-hidden="true"
                  tabindex="-1"
                  data-testid="acl-file-text-input"
                ></textarea>
                <div
                  v-for="(error, index) in fileTextErrors"
                  :key="`fileText-error-${index}`"
                  class="help-block"
                  data-testid="acl-file-text-error"
                >
                  {{ error }}
                </div>
              </div>
            </div>

            <div
              v-if="validation && !validation.valid"
              data-testid="acl-validation-section"
            >
              <h3>
                <i
                  class="glyphicon glyphicon-warning-sign text-warning has_tooltip"
                  role="img"
                  :title="$t('aclpolicy.format.validation.failed')"
                  :aria-label="$t('aclpolicy.format.validation.failed')"
                  data-testid="acl-validation-warning-icon"
                ></i>
                {{ $t("error.validation.failed.title") }}
              </h3>
              <acl-validation-report :validation="validation" />
            </div>
          </div>
        </div>
      </div>

      <div class="card-footer">
        <button
          type="submit"
          name="cancel"
          value="cancel"
          class="btn btn-default"
          data-testid="acl-cancel-button"
          @click="disarmPageConfirmation"
        >
          {{ $t("button.action.Cancel") }}
        </button>
        <button
          type="submit"
          name="save"
          value="save"
          class="btn btn-cta"
          data-testid="acl-save-button"
          @click="disarmPageConfirmation"
        >
          {{ $t("button.action.Save") }}
        </button>
      </div>
    </div>
  </form>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";

import AceEditor from "@/library/components/utils/AceEditor.vue";

import AclValidationReport from "./AclValidationReport.vue";
import type {
  AceEditorInstance,
  AclCsrfToken,
  AclFieldErrors,
  AclHelpLinks,
  AclValidation,
} from "./types/aclFileTypes";

export default defineComponent({
  name: "AclFileEditorForm",
  components: {
    AceEditor,
    AclValidationReport,
  },
  props: {
    project: {
      type: String,
      required: true,
    },
    name: {
      type: String as PropType<string | null>,
      default: null,
    },
    fileText: {
      type: String,
      default: "",
    },
    fieldErrors: {
      type: Object as PropType<AclFieldErrors>,
      default: () => ({}),
    },
    validation: {
      type: Object as PropType<AclValidation | null>,
      default: null,
    },
    saveUrl: {
      type: String,
      required: true,
    },
    backUrl: {
      type: String,
      required: true,
    },
    csrf: {
      type: Object as PropType<AclCsrfToken>,
      required: true,
    },
    helpLinks: {
      type: Object as PropType<AclHelpLinks>,
      required: true,
    },
  },
  data() {
    return {
      localName: this.name || "",
      localFileText: this.fileText,
      helpOpen: false,
      pageConfirmationArmed: false,
    };
  },
  computed: {
    nameErrors(): string[] {
      return this.fieldErrors.name || [];
    },
    fileTextErrors(): string[] {
      return this.fieldErrors.fileText || [];
    },
  },
  mounted() {
    window.addEventListener("beforeunload", this.confirmPageLeave);
    document.addEventListener("click", this.closeHelpOnOutsideClick);
    document.addEventListener("keydown", this.closeHelpOnEscape);
  },
  unmounted() {
    window.removeEventListener("beforeunload", this.confirmPageLeave);
    document.removeEventListener("click", this.closeHelpOnOutsideClick);
    document.removeEventListener("keydown", this.closeHelpOnEscape);
  },
  methods: {
    toggleHelp(): void {
      this.helpOpen = !this.helpOpen;
    },
    closeHelp(): void {
      this.helpOpen = false;
    },
    closeHelpOnOutsideClick(event: MouseEvent): void {
      const dropdown = this.$refs.helpDropdown as HTMLElement | undefined;
      if (!this.helpOpen || !dropdown) {
        return;
      }
      if (!dropdown.contains(event.target as Node)) {
        this.closeHelp();
      }
    },
    closeHelpOnEscape(event: KeyboardEvent): void {
      if (!this.helpOpen || event.key !== "Escape") {
        return;
      }
      this.closeHelp();
      (this.$refs.helpToggle as HTMLButtonElement | undefined)?.focus();
    },
    focusEditor(editor: AceEditorInstance): void {
      editor.focus();
      const editorInput = editor.textInput?.getElement();
      if (editorInput && !editorInput.id) {
        editorInput.id = "acl-policy-editor-input";
      }
    },
    markDirty(): void {
      this.pageConfirmationArmed = true;
    },
    disarmPageConfirmation(): void {
      this.pageConfirmationArmed = false;
    },
    handleSubmit(): void {
      this.disarmPageConfirmation();
    },
    confirmPageLeave(event: BeforeUnloadEvent): string | undefined {
      if (!this.pageConfirmationArmed) {
        return undefined;
      }
      event.preventDefault();
      event.returnValue = this.$t("page.unsaved.changes") as string;
      return event.returnValue;
    },
  },
});
</script>

<style scoped lang="scss">
.acl-card-title {
  min-height: var(--sizes-8);
}

.acl-editor-container {
  --acl-editor-height: var(--sizes-48);

  :deep(.resizable .ace_editor),
  :deep(.ace_editor) {
    height: var(--acl-editor-height);
  }
}

.card-footer {
  display: flex;
  gap: var(--sizes-2);
}
</style>
