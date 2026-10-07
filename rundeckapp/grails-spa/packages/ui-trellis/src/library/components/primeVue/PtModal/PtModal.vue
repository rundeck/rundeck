<template>
  <Dialog
    v-model:visible="visible"
    class="pt-modal"
    modal
    :header="header"
    :closable="closable"
    :close-on-escape="closeOnEscape"
    :dismissable-mask="dismissableMask"
    @show="$emit('show')"
    @hide="$emit('hide')"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="$slots.closebutton" #closebutton="slotProps">
      <slot name="closebutton" v-bind="slotProps" />
    </template>
    <template v-if="$slots.closeicon" #closeicon="slotProps">
      <slot name="closeicon" v-bind="slotProps" />
    </template>
    <template v-if="$slots.container" #container="slotProps">
      <slot name="container" v-bind="slotProps" />
    </template>
    <slot />
    <template v-if="$slots.footer || buttons.length" #footer>
      <slot name="footer">
        <PtButton
          v-for="button in buttons"
          :key="button.id"
          :label="button.label"
          :severity="button.severity"
          :outlined="button.outlined"
          :text="button.text"
          :icon="button.icon"
          :loading="button.loading"
          :disabled="button.disabled"
          :data-testid="`pt-modal-button-${button.id}`"
          @click="$emit('button-click', button.id)"
        />
      </slot>
    </template>
  </Dialog>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Dialog from "primevue/dialog";
import PtButton from "../PtButton/PtButton.vue";
import type { PtModalButton } from "./ptModalTypes";

export default defineComponent({
  name: "PtModal",
  components: { Dialog, PtButton },
  props: {
    /** Whether the modal is open; bind with v-model. */
    modelValue: {
      type: Boolean,
      default: false,
    },
    header: {
      type: String,
      default: undefined,
    },
    /** Footer buttons; clicking one emits `button-click` with its id. */
    buttons: {
      type: Array as PropType<PtModalButton[]>,
      default: () => [],
    },
    closable: {
      type: Boolean,
      default: true,
    },
    closeOnEscape: {
      type: Boolean,
      default: true,
    },
    dismissableMask: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue", "button-click", "show", "hide"],
  computed: {
    visible: {
      get(): boolean {
        return this.modelValue;
      },
      set(value: boolean) {
        this.$emit("update:modelValue", value);
      },
    },
  },
});
</script>

<style lang="scss">
.p-dialog.pt-modal {
  width: var(--sizes-modal-md);
  max-width: 100%;
  border: 0;
  border-radius: var(--radii-lg);
  box-shadow: var(--shadows-base);
  font-family: Inter, var(--fonts-body2);

  .p-dialog-header {
    padding: var(--sizes-8);
  }

  .p-dialog-title {
    font-size: 17.5px;
    font-weight: var(--fontWeights-bold);
    line-height: 21px;
    color: var(--colors-gray-800-original);
  }

  .p-dialog-close-button {
    width: var(--sizes-12);
    height: var(--sizes-12);
    color: var(--colors-gray-800-original);
  }

  .p-dialog-content {
    padding: 0 var(--sizes-8) var(--sizes-12);
    font-size: 14px;
    line-height: 21px;
    color: var(--colors-gray-650);
  }

  .p-dialog-footer {
    gap: var(--sizes-3);
    padding: 0 var(--sizes-8) var(--sizes-8);
  }
}
</style>
