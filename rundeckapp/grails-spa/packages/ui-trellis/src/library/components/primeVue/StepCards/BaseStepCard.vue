<template>
  <Card
    class="baseStepCard"
    :class="[cardClass, { collapsed: !contentExpanded }]"
    data-testid="base-step-card"
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
import StepCardHeader from "./StepCardHeader.vue";

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

<!-- eslint-disable-next-line vue/enforce-style-attribute -- styles PrimeVue internals globally -->
<style lang="scss">
.baseStepCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300-original);

  .p-card-body {
    padding: var(--sizes-4);
  }

  &.collapsed {
    .p-card-body {
      display: none;
    }

    .stepCardHeader {
      border-bottom: 0 !important;
    }
  }
}

/* Dark mode (Figma: Dark Mode/Grey scale) */
*[data-color-theme="dark"] .baseStepCard {
  background: var(--grey-800);
  border-color: var(--grey-400);
  color: var(--white, #fff);

  .p-card-body,
  .p-card-content {
    background: var(--grey-800);
    color: var(--white, #fff);
  }

  .tag-code {
    background: var(--grey-100);
    color: var(--grey-800);
  }

  // The conditional step icon is a dark glyph; flip it so it reads on grey-600
  &.complex > .p-card-header .plugin-icon {
    filter: invert(1);
  }
}
</style>
