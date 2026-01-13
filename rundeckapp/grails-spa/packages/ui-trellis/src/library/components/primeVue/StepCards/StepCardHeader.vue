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
          :value="config.nodeStep ? $t('Workflow.nodeStep') : $t('Workflow.workflowStep')"
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
        :aria-label="$t('Workflow.deleteThisStep')"
        v-tooltip.top="$t('Workflow.deleteThisStep')"
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
        :model="[{ label: $t('Workflow.duplicateStep') }]"
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
  },
  methods: {
    handleMoreActions(event: Event) {
      (this.$refs.menu as any).toggle(event);
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
  border-bottom: 2px solid var(--colors-gray-300);
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
  color: #0052CC;
  cursor: pointer;
  font-weight: 400;
  font-size: 16px;

  + .pi,
  .pi {
    color: #0052CC;
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
</style>
