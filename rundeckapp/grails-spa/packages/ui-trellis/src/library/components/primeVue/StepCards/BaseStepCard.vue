<template>
  <Card
    class="baseStepCard"
    :class="[cardClass, { collapsed: !contentExpanded }]"
  >
    <template #header>
      <slot v-if="$slots.header" name="header" />
      <StepCardHeader
        v-else
        :plugin-details="pluginDetails"
        :config="config"
        :editing="editing"
        :show-as-node-step="showAsNodeStep"
        :show-toggle="showToggle"
        :expanded="contentExpanded"
        :disabled="disabled"
        :validation-errors="validationErrors"
        :show-invalid-condition="showInvalidCondition"
        @delete="$emit('delete')"
        @duplicate="$emit('duplicate')"
        @edit="$emit('edit')"
        @toggle="contentExpanded = !contentExpanded"
      />
    </template>
    <template #content>
      <slot name="content" />
    </template>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";

export default defineComponent({
  name: "BaseStepCard",
  components: {
    Card,
    StepCardHeader,
  },
  props: {
    pluginDetails: {
      type: Object,
      default: () => ({}),
    },
    config: {
      type: Object,
      default: () => ({}),
    },
    serviceName: {
      type: String,
      default: "WorkflowStep",
    },
    showToggle: {
      type: Boolean,
      default: false,
    },
    editing: {
      type: Boolean,
      default: false,
    },
    showAsNodeStep: {
      type: Boolean,
      default: undefined,
    },
    cardClass: {
      type: String,
      default: "",
    },
    initiallyExpanded: {
      type: Boolean,
      default: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    validationErrors: {
      type: Object,
      default: () => ({}),
    },
    showInvalidCondition: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["delete", "duplicate", "edit", "toggle"],
  data() {
    return {
      contentExpanded: this.initiallyExpanded,
    };
  },
});
</script>

<style lang="scss">
.baseStepCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300-original);

  p,
  a,
  span:not(.glyphicon, .fa, .pi) {
    font-family: Inter, var(--fonts-body) !important;
  }

  .p-card-body {
    padding: var(--sizes-4);
  }

  &.collapsed .p-card-body {
    display: none;
  }
}
</style>
