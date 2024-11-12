<template>
  <div>
    <h1 class="title text-h3">
      <i class="fas fa-rocket"></i>
      {{ $t("try.rba") }}
    </h1>
    <div class="card">
      <div class="card-content">
        <Transition>
          <MigrationFirstStep
            v-if="activeStep === 1"
            @next-step="goToNextStep"
          />
          <MigrationValidationStep v-else />
        </Transition>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import MigrationFirstStep from "./MigrationFirstStep.vue";
import MigrationValidationStep from "./MigrationValidationStep.vue";

export default defineComponent({
  name: "MigrationWizard",
  components: { MigrationValidationStep, MigrationFirstStep },
  data() {
    return {
      activeStep: 1,
    };
  },
  methods: {
    goToNextStep() {
      this.activeStep++;
    },
  },
});
</script>

<style lang="scss">
h1,
h2 {
  margin-top: 0;
}
</style>
