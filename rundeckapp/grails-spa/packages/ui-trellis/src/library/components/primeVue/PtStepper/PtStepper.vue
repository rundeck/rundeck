<template>
  <Stepper :value="activeStep" linear>
    <StepList>
      <Step
        v-for="(item, index) in items"
        :key="`step${index}`"
        :value="index + 1"
        :pt="getPtOptions(item)"
      >
        <slot name="step" :item="item">{{ item.label }}</slot>
      </Step>
    </StepList>
    <slot></slot>
  </Stepper>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Stepper from "primevue/stepper";
import StepList from "primevue/steplist";
import Step from "primevue/step";
import { Item } from "./ptStepperTypes";

export default defineComponent({
  name: "PtStepper",
  components: { Stepper, StepList, Step },
  props: {
    activeStep: {
      type: Number,
      default: 1,
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
    getPtOptions(item: Item) {
      return {
        root: ({ context, props }) => {
          return {
            class:
              item.completed ||
              (!context.active && props.value < this.activeStep)
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
.p-step {
  &-header {
    //display: flex;
    //flex-direction: column;

    .p-step-number {
      border-color: var(--colors-gray-500);
      color: var(--colors-gray-500);
      border-width: 1px;
    }
  }

  .p-stepper-separator {
    //background: var(--colors-gray-300);
  }

  &.p-completed {
    opacity: 1;

    .p-step-number {
      background: var(--colors-gray-200);
      border-color: var(--colors-blue-500);
      color: var(--colors-blue-500);
    }

    .p-step-title {
      color: var(--colors-gray-800);
      font-weight: var(--fontWeights-normal);
    }

    .p-stepper-separator {
      background: var(--colors-blue-500);
    }

    &.p-step-active {
      .p-stepper-separator {
        background: var(--p-stepper-separator-background);
      }
    }
  }

  &.p-step-active {
    .p-step-number {
      background: var(--colors-blue-500);
      border-color: var(--colors-blue-500);
      color: var(--colors-white);
    }

    .p-step-title {
      color: var(--colors-gray-800);
      font-weight: var(--fontWeights-bold);
    }
  }

  //&.p-disabled {
  //  opacity: 1;
  //}
}
</style>
