<template>
  <div class="stepCardHeader">
    <div>
      <div>
        <plugin-info
          :detail="{
            ...config,
            ...pluginDetails,
            title: config.description || pluginDetails.description,
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
      <div class="stepCardHeader-description">
        <Tag
          :class="[config.nodeStep ? 'tag-node' : 'tag-workflow']"
          :value="config.nodeStep ? nodeStepLabel : workflowStepLabel"
        />
        <p>{{ pluginDetails.title }}</p>
        <i
          class="pi pi-info-circle"
          v-tooltip="{
            value: pluginDetails.tooltip || pluginDetails.description,
          }"
        ></i>
      </div>
    </div>
    <div class="stepCardHeader-buttons">
      <PtButton
        outlined
        severity="secondary"
        icon="pi pi-trash"
        :aria-label="deleteButtonLabel"
        v-tooltip.top="deleteButtonTooltip"
      />
      <PtButton
        outlined
        severity="secondary"
        icon="pi pi-ellipsis-h"
        aria-haspopup="true"
        aria-controls="overlay_menu"
        @click="handleMoreActions"
      />
      <Menu
        ref="menu"
        id="overlay_menu"
        :model="menuItems"
        popup
      />
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import Menu from "primevue/menu";
import PluginInfo from "../../plugins/PluginInfo.vue";
import PtButton from "../PtButton/PtButton.vue";
import Tag from "primevue/tag";

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
    deleteButtonLabel: {
      type: String,
      default: "Delete",
    },
    deleteButtonTooltip: {
      type: String,
      default: "Delete this step",
    },
    menuItems: {
      type: Array,
      default: () => [{ label: "Duplicate" }],
    },
    nodeStepLabel: {
      type: String,
      default: "Node Step",
    },
    workflowStepLabel: {
      type: String,
      default: "Workflow Step",
    },
  },
  methods: {
    handleMoreActions(event: Event) {
      (this.$refs.menu as any).toggle(event);
    },
  },
});
</script>

<style scoped lang="scss">
.stepCardHeader {
  background-color: var(--colors-secondaryBackgroundOnLight);
  border-bottom: 2px solid var(--colors-gray-300);
  display: flex;
  justify-content: space-between;
  padding: var(--sizes-4);

  p,
  a,
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  :deep(.plugin-info) {
    display: flex;
    align-items: center;
  }

  :deep(.plugin-icon) {
    height: 16px !important;
  }

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
  }
}
</style>
