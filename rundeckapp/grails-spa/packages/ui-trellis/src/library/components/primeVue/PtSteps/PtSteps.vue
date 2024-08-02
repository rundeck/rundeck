<template>
  <Steps :active-step="activeStep" :model="items" :pt="getPtOptions()"></Steps>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Steps from "primevue/steps";
import { Item } from "./ptStepTypes";

export default defineComponent({
  name: "PtSteps",
  components: { Steps },
  props: {
    activeStep: {
      type: Number,
      default: 0,
    },
    items: {
      type: Array as PropType<Item[]>,
      required: true,
      validator: (arrayOfObjects: any) => {
        return arrayOfObjects.every((item: any) => {
          return item.label;
        });
      },
    },
  },
  methods: {
    getPtOptions() {
      return {
        menuItem: ({ context }) => {
          return {
            class:
              context.index < this.activeStep || context.item.completed
                ? "p-completed"
                : "",
          };
        },
      };
    },
  },
});
</script>

<style lang="scss">
.p-steps {
  .p-steps-item {
    // timeline bar
    &:before {
      border-color: var(--colors-gray-500);
    }

    .p-steps-number {
      border-color: var(--colors-gray-500);
      color: var(--colors-gray-500);
    }

    &.p-disabled {
      opacity: 1;
    }

    // completed step
    &.p-completed {
      opacity: 1;

      .p-steps-number {
        background: var(--colors-blue-500);
        border-color: var(--colors-blue-500);
        color: var(--colors-white);
      }

      .p-steps-title {
        color: var(--colors-gray-800);
        font-weight: var(--fontWeights-normal);
      }
    }

    // active step
    &.p-highlight {
      .p-steps-number {
        background: var(--colors-gray-200);
        border-color: var(--colors-blue-500);
        color: var(--colors-blue-500);
      }

      .p-steps-title {
        color: var(--colors-gray-800);
      }
    }
  }
}
</style>
