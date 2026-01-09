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
      <div class="conditionalCard--header-description">
        <Tag
          :class="[config.nodeStep ? 'tag-node' : 'tag-workflow']"
          :value="`${config.nodeStep ? 'Node' : 'Workflow'} Step`"
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
        aria-label="Delete"
        v-tooltip.top="'Delete this step'"
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
        :model="[{ label: 'Duplicate' }]"
        popup
      />
    </div>
  </div>
</template>
<script lang="ts">
import Menu from "primevue/menu";
import PluginInfo from "../../plugins/PluginInfo.vue";
import PtButton from "../PtButton/PtButton.vue";
import Tag from "primevue/tag";

export default {
  name: "StepCardHeader",
  components: { Menu, PluginInfo, PtButton, Tag },
  props: {
    pluginDetails: {
      type: Object,
      default: () => ({
        iconUrl: "./public/library/theme/images/icon-condition.png",
        title: "For each Linux node",
        description: "Conditional Logic on Node Step",
        tooltip: "Only linux nodes will execute the following steps",
      }),
    },
    config: {
      type: Object,
      required: true,
    },
  },
  methods: {
    handleMoreActions(event) {
      this.$refs.menu.toggle(event);
    },
  },
};
</script>

<style scoped lang="scss">
.stepCardHeader {
  p,
  a,
  span:not(.glyphicon, .fa) {
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
</style>
