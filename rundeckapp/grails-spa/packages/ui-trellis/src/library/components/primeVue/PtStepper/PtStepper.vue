<template>
  <Stepper
    v-if="vertical"
    :value="activeStep"
    linear
    class="pt-stepper-vertical"
    data-testid="pt-stepper-vertical"
  >
    <StepItem
      v-for="(item, index) in items"
      :key="`step${index}`"
      :value="index + 1"
    >
      <Step :value="index + 1" :pt="getPtOptions(item)">
        <slot name="step" :item="item">
          <span :data-testid="`pt-stepper-label-${index + 1}`">{{
            item.label
          }}</span>
        </slot>
      </Step>
      <div
        class="pt-stepper-content"
        :data-testid="`pt-stepper-content-${index + 1}`"
      >
        <slot name="content" :item="item" :index="index"></slot>
      </div>
    </StepItem>
  </Stepper>
  <Stepper
    v-else
    :value="activeStep"
    linear
    data-testid="pt-stepper-horizontal"
  >
    <StepList>
      <Step
        v-for="(item, index) in items"
        :key="`step${index}`"
        :value="index + 1"
        :pt="getPtOptions(item)"
      >
        <slot name="step" :item="item">
          <span :data-testid="`pt-stepper-label-${index + 1}`">{{
            item.label
          }}</span>
        </slot>
      </Step>
    </StepList>
    <slot></slot>
  </Stepper>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Stepper from "primevue/stepper";
import StepList from "primevue/steplist";
import Step, { type StepPassThroughMethodOptions } from "primevue/step";
import StepItem from "primevue/stepitem";
import { Item } from "./ptStepperTypes";

export default defineComponent({
  name: "PtStepper",
  components: { Stepper, StepList, Step, StepItem },
  props: {
    /**
     * Layout of the steps. `vertical` stacks every step with its `content`
     * slot underneath, joined by a connector line; all content stays visible.
     */
    orientation: {
      type: String as PropType<"horizontal" | "vertical">,
      default: "horizontal",
      validator: (value: string) => ["horizontal", "vertical"].includes(value),
    },
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
  computed: {
    vertical(): boolean {
      return this.orientation === "vertical";
    },
  },
  methods: {
    getPtOptions(item: Item) {
      return {
        root: ({ context, props }: StepPassThroughMethodOptions) => {
          return {
            class:
              item.completed ||
              (!context.active && Number(props.value) < this.activeStep)
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
    //background: var(--colors-gray-300-original);
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

// Vertical timeline (all steps and their content visible at once)
.pt-stepper-vertical {
  .p-stepitem {
    flex: initial;
  }

  .p-step {
    padding: 0;

    &.p-disabled {
      opacity: 1;
    }
  }

  .p-step-header {
    padding: 0;
    font-family: Inter, var(--fonts-body2);
    gap: 8px;
    cursor: default;

    .p-step-number {
      min-width: 24px;
      height: 24px;
      line-height: 24px;
      border: 0;
      border-radius: 50%;
      background: var(--colors-gray-150, #f4f4f5);
      font-size: 14px;
      font-weight: var(--fontWeights-normal);
      color: var(--colors-gray-800);
    }

    .p-step-title {
      font-size: 16px;
      font-weight: 500;
      color: var(--colors-gray-800-original);
    }
  }

  // connector line, centered under the number circle
  .pt-stepper-content {
    margin: 4px 0 4px 11px;
    padding: 12px 0 12px 20px;
    border-left: 1px solid var(--colors-gray-400);
  }

  .p-stepitem:last-of-type .pt-stepper-content {
    border-left-color: transparent;
  }
}
</style>
