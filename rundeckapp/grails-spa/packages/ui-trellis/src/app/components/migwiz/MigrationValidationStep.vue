<template>
  <div
    class="flex-container flex-align-items-flex-end flex-justify-end step-container"
  >
    <p>{{ $t("migwiz.waitForActivation") }}</p>
    <TransitionGroup name="fade" tag="div" class="flex-col progress-container">
      <div
        v-for="(item, index) in items"
        :key="item.step"
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
    </TransitionGroup>
    <button
      type="submit"
      class="btn btn-submit"
      :disabled="!allStepsConcluded"
      @click="next"
    >
      {{ $t("migwiz.nextStep") }}
    </button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "MigrationValidationStep",
  emits: ["nextStep"],
  data() {
    return {
      iconsBase: {
        notStarted: "far fa-circle",
        loading: "fas fa-spinner fa-spin",
        ready: "fas fa-check-circle",
      },
      items: [
        {
          state: "notStarted",
          step: "instance",
        },
        {
          state: "notStarted",
          step: "validation",
        },
        {
          state: "notStarted",
          step: "runner",
        },
      ],
    };
  },
  computed: {
    allStepsConcluded() {
      return this.items.every((item) => item.state === "ready");
    },
  },
  mounted() {
    this.simulateTransitions();
  },
  methods: {
    next() {
      this.$emit("nextStep", {
        isValid: true,
      });
    },
    simulateTransitions() {
      const transitionItem = (index) => {
        if (index >= this.items.length) return;

        setTimeout(() => {
          this.items[index].state = "loading";

          setTimeout(() => {
            this.items[index].state = "ready";
            transitionItem(index + 1);
          }, 2000);
        }, 1000);
      };

      transitionItem(0);
    },
  },
});
</script>

<style scoped lang="scss">
.progress-container {
  gap: 10px;
}

.progress-item {
  gap: 15px;
  transition: all 0.3s ease;

  &.notStarted {
    opacity: 40%;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

.fade-enter-to,
.fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

i {
  transition: all 0.3s ease;
}

p {
  transition: all 0.3s ease;
}
</style>
