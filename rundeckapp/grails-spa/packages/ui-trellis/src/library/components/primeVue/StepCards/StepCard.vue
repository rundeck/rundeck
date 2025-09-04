<template>
  <Card class="stepCard">
    <template #header>
      <StepCardHeader :handle-more-actions="handleMoreActions"/>
    </template>
    <template #content>
      <ConfigSection
        title="Log Filters"
        tooltip="Only linux nodes will execute the following steps"
        v-model="logFilters"
        @addElement="handleAddElement"
      />
      <ConfigSection
          title="Error Handler"
          tooltip="Only linux nodes will execute the following steps"
          v-model="errorHandler"
          @addElement="handleAddElement"
      >
        test
      </ConfigSection>
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import ConfigSection from "@/library/components/primeVue/StepCards/ConfigSection.vue";

export default defineComponent({
  name: "StepCard",
  components: {
    Card,
    StepCardHeader,
    ConfigSection,
  },
  props: {},
  data() {
    return {
      logFilters: [],
      errorHandler: []
    }
  },
  methods: {
    handleMoreActions() {
      // Handle more actions logic
    },
    handleAddElement() {
      this.logFilters.push({
        "name": "quiet-output",
        "title": "Quiet Output", 
        "description": "Quiets all output which does or does not match a certain pattern by changing its log level.",
        "providerMetadata": {"faicon": "volume-off"}
      });
    },
  },
});
</script>

<style lang="scss">
.stepCard {
  box-shadow: none;
  overflow: hidden;
  border-radius: var(--radii-md);
  border: 1px solid var(--colors-gray-300);

  p,
  a,
  span:not(.glyphicon, .fa) {
    font-family: Inter, var(--fonts-body) !important;
  }
}
</style>
