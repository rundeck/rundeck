<template>
  <div class="flex-container flex-align-items-flex-end flex-justify-end">
    <p>{{ $t("migwiz.waitForActivation") }}</p>
    <div class="flex-col progress-container">
      <div
        v-for="(item, index) in items"
        class="flex flex--items-center progress-item"
        :class="item.state"
      >
        <i class="fa-lg" :class="iconsBase[item.state]"></i>
        <p class="panel-title text-h5">
          {{
            $t(
              `migwiz.setup.inprogress.step${index + 1}${item.state === "ready" ? ".ready" : ""}`,
            )
          }}
        </p>
      </div>
    </div>
    <btn :disabled="!allStepsConcluded">
      {{ $t("migwiz.accessYourTrial") }}
    </btn>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "MigrationValidationStep",
  data() {
    return {
      iconsBase: {
        notStarted: "far fa-circle",
        loading: "fas fa-spinner fa-spin",
        ready: "fas fa-check-circle",
      },
      base: [
        {
          state: "ready",
          step: "instance",
        },
        {
          state: "loading",
          step: "validation",
        },
        {
          state: "notStarted",
          step: "runner",
        },
        {
          state: "notStarted",
          step: "import",
        },
      ],
    };
  },
  computed: {
    icons() {
      return this.loading;
    },
    items() {
      return this.base;
    },
    allStepsConcluded() {
      return this.items.every((item) => item.ready);
    },
  },
});
</script>

<style scoped lang="scss">
.progress-container {
  gap: 10px;
}
.progress-item {
  gap: 10px;

  &.notStarted {
    opacity: 40%;
  }
}
</style>
