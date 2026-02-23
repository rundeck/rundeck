<template>
  <Message icon="pi pi-star-fill">
    <p>{{ $t('earlyAccess') }}</p>
    <p class="text-body">{{ $t('earlyAccessDescriptionWorkflow') }}</p>
    <PtButton
      outlined
      severity="secondary"
      :label="$t('earlyAccessRemindMeLater')"
    ></PtButton>
    <PtButton :label="$t('earlyAccessTryNow')"></PtButton>
  </Message>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Message from "primevue/message";
import "../../../../library/components/primeVue/Message/message.scss";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";

export default defineComponent({
  name: "Banner",
  components: {
    PtButton,
    Message,
  },
  props: {
    severity: {
      type: String,
    },
    icon: {
      type: String,
    },
  },
});
</script>

<style scoped lang="scss">
p {
  margin-bottom: 0 !important;
}

.text-body {
  flex: 1;
}

.p-message {
  margin-bottom: 10px;
}

:deep(.p-message-content) {
  align-items: center;
  display: flex;
  width: 100%;
  padding: 17px 24px;
}

:deep(.p-message-text) {
  gap: 7px;
  width: 100%;
}
</style>
