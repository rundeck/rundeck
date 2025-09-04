<template>
  <Card class="stepCard">
    <template #header>
      <StepCardHeader :plugin-details="pluginDetails" :config="pdStep" />
    </template>
    <template #content>
      <StepCardContent />
    </template>
  </Card>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Card from "primevue/card";
import StepCardHeader from "@/library/components/primeVue/StepCards/StepCardHeader.vue";
import StepCardContent from "@/library/components/primeVue/StepCards/StepCardContent.vue";

export default defineComponent({
  name: "StepCard",
  components: {
    Card,
    StepCardHeader,
    StepCardContent,
  },
  props: {},
  data() {
    return {
      pluginDetails: {
        title: "PagerDuty / User / Create",
        description: "Create a user",
        iconUrl: "./public/library/theme/images/icon-pagerduty.png",
      },
      pdStep: {
        description: "Add new responder in PD",
        nodeStep: false,
        jobref: undefined,
        type: "pagerduty-create-user",
        config: {
          email: "janedoe@pagerduty.com",
          name: "Jane Doe",
          color: "green",
          role: "admin",
          title: "Responder",
          description:
            "Loves uneventful days, but always ready to hop on a call to help the team. On their free time, they like to spend as much time as possible in the nature with family.",
          apiKey: "keys/example/exampleKey.key",
        },
      },
    };
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

  .p-card-body {
    padding: var(--sizes-4);
  }

}
</style>
