<template>
  <div class="conditional">
    <template v-if="conditionSet && conditionSet.length > 0">
      <template v-for="(conditionSet, setIndex) in conditionSet" :key="conditionSet.id">
        <p v-if="setIndex === 0" class="conditional--if">
          {{ $t("editConditionalStep.if") || "If" }}
          <template v-for="(condition, condIndex) in conditionSet.conditions" :key="condition.id">
            <Tag v-if="condition.field" class="tag-code" :value="condition.field" />
            <span v-if="condition.field" class="conditional--equality">{{ getOperatorLabel(condition.operator) }}</span>
            <span v-if="condition.field" class="bold">{{ condition.value }}</span>
            <span v-if="condIndex < conditionSet.conditions.length - 1" class="operator--and">{{ $t("editConditionalStep.and") }}</span>
          </template>
        </p>
      </template>
    </template>
    <div v-if="!headerOnly" class="conditional--do">
      <div v-if="complex && conditionSet && conditionSet.length > 1" class="conditional--complex">
        <template v-for="(conditionSet, setIndex) in conditionSet" :key="conditionSet.id">
          <template v-if="setIndex > 0">
            <p class="conditional--or">
              <span>{{ $t("editConditionalStep.or") }}</span><span class="conditional--divider" />
            </p>
            <p class="conditional--if">
              <template v-for="(condition, condIndex) in conditionSet.conditions" :key="condition.id">
                <Tag v-if="condition.field" class="tag-code" :value="condition.field" />
                <span v-if="condition.field" class="conditional--equality">{{ getOperatorLabel(condition.operator) }}</span>
                <span v-if="condition.field" class="bold">{{ condition.value }}</span>
                <span v-if="condIndex < conditionSet.conditions.length - 1" class="operator--and">{{ $t("editConditionalStep.and") }}</span>
              </template>
            </p>
          </template>
        </template>
        <div class="conditional--connector"></div>
      </div>
      <div v-else class="conditional--connector"></div>
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import Tag from "primevue/tag";
import type { ConditionSet } from "@/app/components/job/workflow/types/conditionalStepTypes";

export default defineComponent({
  name: "Conditional",
  components: {
    Tag,
  },
  props: {
    complex: {
      type: Boolean,
      default: false
    },
    conditionSet: {
      type: Array as PropType<ConditionSet[]>,
      default: () => []
    },
    headerOnly: {
      type: Boolean,
      default: false
    },
  },
  methods: {
    getOperatorLabel(operator: string): string {
      const operatorMap: Record<string, string> = {
        equals: this.$t("Workflow.conditional.operator.equals") as string,
        notEquals: this.$t("Workflow.conditional.operator.notEquals") as string,
        contains: this.$t("Workflow.conditional.operator.contains") as string,
        notContains: this.$t("Workflow.conditional.operator.notContains") as string,
        regex: this.$t("Workflow.conditional.operator.regex") as string,
      };
      return operatorMap[operator] || operator;
    },
  },
});
</script>

<style lang="scss">
.conditional {
  counter-reset: step;
  width: 100%;

  &--if {
    align-items: baseline;
    display: flex;
    gap: 6px;
    margin: 0;

    .operator--and {
      color: var(--colors-cyan-700);
    }
  }

  &--do {
    padding-left: 30px;
    position: relative;
  }

  &--connector {
    border-bottom: 1px solid var(--colors-gray-400);
    border-left: 1px solid var(--colors-gray-400);
    border-radius: 0 var(--radii-lg) 0;
    position: absolute;
    top: 3px;
    left: 3px;
    height: 25px;
    //width: 25px;
    width: 23px;
  }

  &--complex {
    position: relative;
    display: flex;
    flex-direction: column;
    margin-top: var(--sizes-3);
    margin-left: -15px;

    .conditional--or {
      align-items: center;
      color: var(--colors-pink-600);
      display: flex;
      font-weight: var(--fontWeights-semibold);
      gap: 10px;
    }

    .conditional--if {
      margin-bottom: var(--sizes-3);
    }

    .conditional--divider{
      background: var(--colors-gray-300-original);
      display: flex;
      height: 1px;
      width: 100%;
      //margin-right: var(--sizes-3);
    }

    .conditional--connector {
      height: calc(100% + 25px);
      left: -13px;
    }
  }

  .bold {
    font-weight: var(--fontWeights-semibold);
  }

  .p-accordionheader {
    background: none;
    border-left: none;
    border-top: none;
    border-right: none;
    border-radius: 0;
    font-weight: var(--fontWeights-normal);
    gap: var(--sizes-2);
    justify-content: flex-start;
    margin-left: var(--sizes-8);
    padding-left: 0;
    z-index: 1;

    &.nested {
      padding-right: 0;

      &:last-child {
        padding-bottom: 0;
        border: none;
        cursor: default;
      }
    }

    &.inner-child {
      padding-bottom: 0;
      border: none;
    }

    .conditional {
      margin-right: -1rem;
      margin-top: 1px;
    }

    &::before {
      align-items: center;
      background: var(--colors-cardNumber);
      border-radius: 50%;
      counter-increment: step;
      content: counter(step);
      display: flex;
      flex-shrink: 0;
      font-weight: var(--fontWeights-normal) !important;
      height: 1.5rem;
      justify-content: center;
      left: 0;
      position: absolute;
      top: 1.125rem;
      width: 1.5rem;
    }
  }

  .p-accordioncontent {
    margin-left: var(--sizes-8);

    &-content {
      border-radius: 0 0 5px 5px;
    }
  }

  .p-accordion > .p-accordionpanel {
    position: relative;

    &::before {
      display: block;
      width: 1px;
      height: 100%;
      background: var(--colors-gray-400);
      content: "";
      position: absolute;
      bottom: 0;
      left: 10px;
      z-index: 0;
    }

    &:nth-child(1)::before {
      top: 25px;
    }

    &:last-child::before {
      top: 0;
      height: 1.125rem;
    }

    &.align-start {
      &:last-child::before {
        height: 1.125rem;
      }

      .p-accordionheader {
        align-items: flex-start;
      }
    }

    &:only-child::before {
      display: none;
    }
  }
}
</style>
