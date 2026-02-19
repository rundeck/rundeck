<template>
  <div class="stepCardHeader">
    <div>
      <div>
        <div class="plugin-info-wrapper" :class="{ 'disabled': disabled }" data-testid="step-card-header-plugin-info" @click="handleEdit">
          <plugin-info
            :detail="{
              ...config,
              ...pluginDetails,
              title: config.description || pluginDetails.title,
            }"
            :show-description="false"
            :show-extended="false"
            titleCss="link-title"
          >
            <template #descriptionprefix>
              <i class="pi pi-pencil"/>
            </template>
          </plugin-info>
        </div>
      </div>
      <div class="stepCardHeader-description">
        <p v-if="editing" data-testid="step-card-header-plugin-desc">{{ pluginDetails.description }}</p>
        <template v-else>
          <Tag
            :class="[effectiveNodeStep ? 'tag-node' : 'tag-workflow']"
            :value="effectiveNodeStep ? $t('Workflow.nodeStep') : $t('Workflow.workflowStep')"
            data-testid="step-card-header-step-type-tag"
          />
          <template v-if="config.description">
            <p>{{ pluginDetails.title }}</p>
            <i
              class="pi pi-info-circle"
              v-tooltip="{
                value: pluginDetails.tooltip || pluginDetails.description,
              }"
            ></i>
          </template>
          <p v-else data-testid="step-card-header-plugin-desc-fallback">{{ pluginDetails.description }}</p>
        </template>
      </div>
    </div>
    <div class="stepCardHeader-buttons">
      <Tag
        v-if="showErrorTag && editing"
        icon="pi pi-exclamation-triangle"
        severity="danger"
        :value="showInvalidCondition ? $t('Workflow.validation.invalidConditionCriteria') : errorMessage"
        data-testid="step-card-header-error-tag"
      />
      <PtButton
        outlined
        severity="secondary"
        icon="pi pi-trash"
        :aria-label="$t('Workflow.deleteThisStep')"
        v-tooltip.top="$t('Workflow.deleteThisStep')"
        :disabled="disabled"
        data-testid="step-card-header-delete-btn"
        @click="handleDelete"
      />
      <template v-if="showToggle">
        <PtButton
          text
          :icon="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-up'"
          :aria-label="expanded ? $t('Workflow.collapse') : $t('Workflow.expand')"
          :aria-expanded="expanded"
          :disabled="disabled"
          data-testid="step-card-header-toggle-btn"
          @click.stop="$emit('toggle')"
        />
      </template>
      <template v-else>
        <PtButton
          outlined
          severity="secondary"
          icon="pi pi-ellipsis-h"
          aria-haspopup="true"
          aria-controls="overlay_menu"
          :disabled="disabled"
          data-testid="step-card-header-more-btn"
          @click="handleMoreActions"
        />
        <Menu
          ref="menu"
          id="overlay_menu"
          :model="menuItems"
          popup
        />
      </template>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import Menu from "primevue/menu";
import PluginInfo from "../../plugins/PluginInfo.vue";
import PtButton from "../PtButton/PtButton.vue";
import Tag from "primevue/tag";
import "../Tooltip/tooltip.scss";

export default defineComponent({
  name: "StepCardHeader",
  components: { Menu, PluginInfo, PtButton, Tag },
  props: {
    pluginDetails: {
      type: Object,
      required: true,
    },
    config: {
      type: Object,
      required: true,
    },
    editing: {
      type: Boolean,
      default: false,
    },
    showAsNodeStep: {
      type: Boolean,
      default: undefined,
    },
    showToggle: {
      type: Boolean,
      default: false,
    },
    expanded: {
      type: Boolean,
      default: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    showInvalidCondition: {
      type: Boolean,
      default: false,
    },
    validationErrors: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ["delete", "duplicate", "edit", "toggle"],
  inject: {
    editModelValidation: {
      from: 'editModelValidation',
      default: undefined
    }
  },
  computed: {
    effectiveNodeStep(): boolean {
      if (this.showAsNodeStep !== undefined) {
        return this.showAsNodeStep;
      }
      return !!this.config?.nodeStep;
    },
    menuItems() {
      return [
        {
          label: this.$t('Workflow.duplicateStep'),
          command: () => {
            this.handleDuplicate();
          },
        },
      ];
    },
    errorCount(): number {
      // Only use injected editModelValidation when in editing mode
      // This prevents non-editing StepCards from subscribing to global validation state
      const validation = this.validationErrors?.valid !== undefined
        ? this.validationErrors
        : (this.editing ? this.editModelValidation : null);

      if (!validation || validation.valid) {
        return 0;
      }

      // Special handling for nested conditional logic errors
      if (validation.errors.conditions) {
        let count = 0;
        Object.values(validation.errors.conditions).forEach((conditionError: any) => {
          count += Object.keys(conditionError).length;
        });
        return count;
      }

      return Object.keys(validation.errors).length;
    },
    errorMessage(): string {
      if (this.errorCount === 1) {
        return this.$t('Workflow.validation.oneError');
      }
      return this.$t('Workflow.validation.multipleErrors', { count: this.errorCount });
    },
    showErrorTag(): boolean {
      return this.errorCount > 0;
    },
  },
  methods: {
    handleMoreActions(event: Event) {
      (this.$refs.menu as any).toggle(event);
    },
    handleDelete() {
      this.$emit("delete");
    },
    handleDuplicate() {
      this.$emit("duplicate");
    },
    handleEdit() {
      if (this.disabled) return;
      this.$emit("edit");
    },
  },
});
</script>

<style lang="scss">
.stepCardHeader {
  p,
  a,
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  .plugin-info-wrapper {
    cursor: pointer;

    &.disabled {
      cursor: not-allowed;
      opacity: 0.6;

      .link-title {
        cursor: not-allowed;
        pointer-events: none;
      }
    }
  }

  .plugin {
    &-info {
      display: flex;
      align-items: center;
    }
    &-icon {
      height: 16px !important;
    }
  }

  background-color: var(--colors-secondaryBackgroundOnLight);
  border-bottom: 2px solid var(--colors-gray-300-original);
  display: flex;
  justify-content: space-between;
  padding: var(--sizes-4);

  &-description {
    align-items: baseline;
    display: flex;
    flex-direction: row;
    gap: var(--sizes-1);
    margin-top: var(--sizes-3);

    p {
      font-weight: var(--fontWeights-medium);
      font-size: var(--fontSizes-md);
      margin: 0;
    }
  }

  &-buttons {
    align-items: flex-start;
    display: flex;
    gap: var(--sizes-2);

    .p-button:disabled,
    button:disabled {
      cursor: not-allowed;
      pointer-events: auto;
    }
  }
}

/* Tag styling for Node Step and Workflow Step badges */
.stepCardHeader .p-tag {
  font-family: Inter, var(--fonts-body) !important;
  font-size: 12px;
  font-weight: var(--fontWeights-regular);
  line-height: normal;
  padding: 4px 8px;
  border-radius: 6px;
}

.stepCardHeader .tag-node {
  background-color: var(--colors-blue-100);
  color: var(--colors-gray-900);
}

.stepCardHeader .tag-workflow {
  background-color: var(--colors-green-100);
  color: var(--colors-gray-900);
}

/* Link title styles with hover behavior for pencil icon */
.link-title {
  all: unset;
  color: var(--colors-blue-500);
  cursor: pointer;
  font-weight: 400;
  font-size: 16px;

  + .pi,
  .pi {
    color: var(--colors-blue-500);
    display: none;
    margin-left: 5px;
  }

  &:hover {
    text-decoration: underline;

    + .pi {
      display: inline;
    }
  }
}

/* Menu item styling for dropdown menus */
.p-menu-item {
  color: var(--colors-menuitem-color);
  background: transparent;

  &-link {
    color: inherit !important;
  }
}
</style>
