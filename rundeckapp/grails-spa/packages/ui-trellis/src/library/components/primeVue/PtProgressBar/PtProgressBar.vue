<template>
  <div class="pt-progress-bar-wrapper">
    <ProgressBar
      data-testid="pt-progress-bar"
      class="pt-progress-bar"
      :value="clampedValue"
      :mode="mode"
      :show-value="showValue"
      :aria-label="ariaLabel"
      :aria-labelledby="ariaLabelledby"
    >
      <template v-if="$slots.default">
        <slot />
      </template>
    </ProgressBar>
    <span
      v-if="mode === 'determinate'"
      class="sr-only"
      data-testid="pt-progress-bar-sr"
      >{{ $t("progress.bar.percent.complete", [clampedValue]) }}</span
    >
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ProgressBar from "primevue/progressbar";

/**
 * PtProgressBar wraps PrimeVue's ProgressBar with Rundeck styling.
 *
 * Unlike the raw PrimeVue component, the value is clamped to the 0-100 range
 * (PrimeVue sets the width to `value%` without clamping, so values above 100
 * overflow the track). A visually hidden "% Complete" label is rendered for
 * screen readers in determinate mode.
 */
export default defineComponent({
  name: "PtProgressBar",
  components: { ProgressBar },
  props: {
    /** Current progress, 0-100. Values outside the range are clamped. */
    value: {
      type: Number as PropType<number | null>,
      default: null,
    },
    /** Determinate shows a value; indeterminate shows an animated bar. */
    mode: {
      type: String as PropType<"determinate" | "indeterminate">,
      default: "determinate",
      validator: (val: string) =>
        ["determinate", "indeterminate"].includes(val),
    },
    /** Whether to render the "N%" label inside the bar. */
    showValue: {
      type: Boolean,
      default: true,
    },
    /** Accessible label for the progressbar element. */
    ariaLabel: {
      type: String,
      default: undefined,
    },
    /** Id of the element that labels the progressbar. */
    ariaLabelledby: {
      type: String,
      default: undefined,
    },
  },
  computed: {
    /**
     * Value clamped to 0-100, or null when no value is set.
     * @returns {number | null} clamped value
     */
    clampedValue(): number | null {
      if (this.value === null || this.value === undefined) {
        return null;
      }
      return Math.min(100, Math.max(0, this.value));
    },
  },
});
</script>

<style scoped lang="scss">
.pt-progress-bar-wrapper {
  position: relative;
}

.pt-progress-bar {
  height: var(--sizes-5);
  border-radius: var(--radii-md);
  background: var(--colors-gray-200);

  :deep(.p-progressbar-value) {
    background: var(--colors-blue-500);
    border-radius: var(--radii-md);
  }

  :deep(.p-progressbar-label) {
    color: var(--colors-white);
    font-weight: var(--fontWeights-normal);
    font-size: 12px;
    line-height: 20px;
  }
}

.sr-only {
  position: absolute;
  width: var(--sizes-px);
  height: var(--sizes-px);
  margin: calc(var(--sizes-px) * -1);
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
  white-space: nowrap;
}
</style>
