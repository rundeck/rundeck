<template>
  <div>
    <h1 class="title text-h3">
      <i class="fas fa-rocket"></i>
      {{ $t("try.rba") }} - {{ $t(`migwiz.step${activeStep}.title`) }}
    </h1>
    <div class="card">
      <div class="card-content">
        <Transition name="fade" appear>
          <MigrationFirstStep
            v-if="activeStep === 1"
            @next-step="goToNextStep"
          />
          <MigrationDataStep
            v-else-if="activeStep === 2"
            @next-step="goToNextStep"
          />
          <MigrationValidationStep
            v-else-if="activeStep === 3"
            @next-step="goToNextStep"
          />
          <MigrationConclusion v-else :instance-url="instanceUrl" />
        </Transition>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import MigrationFirstStep from "./MigrationFirstStep.vue";
import MigrationValidationStep from "./MigrationValidationStep.vue";
import MigrationDataStep from "./MigrationDataStep.vue";
import MigrationConclusion from "./MigrationConclusion.vue";

export default defineComponent({
  name: "MigrationWizard",
  components: {
    MigrationConclusion,
    MigrationDataStep,
    MigrationValidationStep,
    MigrationFirstStep,
  },
  data() {
    return {
      activeStep: 1,
      form: [],
      instanceUrl: "",
    };
  },
  methods: {
    goToNextStep(formData) {
      if (
        formData?.data &&
        Object.keys(formData.data).includes("instanceName")
      ) {
        this.instanceUrl = `https://${formData.data.instanceName}.runbook.pagerduty.cloud`;
      }
      this.form.push(formData);
      this.activeStep++;
    },
  },
});
</script>

<style lang="scss">
.card {
  .card-content {
    padding: 20px;
  }
}

h1,
h2 {
  margin-top: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.5s ease-in-out 0.1s,
    height 0.4s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  height: 0;
  overflow: hidden;
}

.step-container {
  gap: 20px;
}
.btn {
  margin-right: 15px;
}
</style>
