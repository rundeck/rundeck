<template>
  <div class="stepCardHeader">
    <div>
      <div>
        <plugin-info
          :detail="{
            iconUrl: 'public/images/icon-condition.png',
            title: 'For each Linux node',
          }"
          :show-description="false"
          :show-extended="false"
        />
      </div>
      <div class="conditionalCard--header-description">
        <Tag class="tag-node" value="Node Step" />
        <p>Conditional Logic on Node Step</p>
        <i
          class="fa fa-info-circle"
          v-tooltip="{
            value: 'Only linux nodes will execute the following steps',
          }"
        ></i>
      </div>
    </div>
    <div class="stepCardHeader-buttons">
      <PtButton
        outlined
        severity="secondary"
        icon="glyphicon glyphicon-trash"
        aria-label="Delete"
        v-tooltip.top="'Delete this step'"
      />
      <PtButton
        outlined
        severity="secondary"
        icon="fa fa-ellipsis-h"
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
  methods: {
    handleMoreActions(event) {
      this.$refs.menu.toggle(event);
    },
  }
};
</script>

<style scoped lang="scss">
.stepCardHeader {
  p,
  a,
  span:not(.glyphicon, .fa) {
    font-family: Inter, var(--fonts-body) !important;
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
