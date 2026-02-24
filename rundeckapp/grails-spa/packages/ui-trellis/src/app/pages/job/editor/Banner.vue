<template>
  <Message
    v-if="shouldShowBanner"
    data-testid="banner-message"
    icon="pi pi-star-fill"
    :closable="isBannerReminded"
    @close="handleClose"
  >
    <p data-testid="banner-title">{{ $t("earlyAccess") }}</p>
    <p class="text-body" data-testid="banner-description">{{ $t("earlyAccessDescriptionWorkflow") }}</p>
    <PtButton
      v-if="!isBannerReminded"
      data-testid="banner-remind-later-button"
      outlined
      severity="secondary"
      :label="$t('earlyAccessRemindMeLater')"
      @click="handleRemindMeLater"
    ></PtButton>
    <PtButton
      data-testid="banner-try-now-button"
      :label="$t('earlyAccessTryNow')"
      @click="handleTryNow"
    ></PtButton>
  </Message>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Message from "primevue/message";
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";
import { getFeatureEnabled } from "@/library/services/feature";
import { loadJsonData } from "@/app/utilities/loadJsonData";
import { Notification } from "uiv";
import {getRundeckContext} from "../../../../library";
import "../../../../library/components/primeVue/Message/message.scss";

const COOKIE_REMINDED = "conditionalEarlyAccessBannerReminded";
const COOKIE_PERMANENTLY_DISMISSED =
  "conditionalEarlyAccessBannerPermanentlyDismissed";

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
  data() {
    return {
      isBannerReminded: false,
      isBannerPermanentlyDismissed: false,
      isFeatureEnabled: false,
      isAdmin: false,
      userActed: false,
      eventBus: getRundeckContext().eventBus,
    };
  },
  computed: {
    shouldShowBanner(): boolean {
      if (
        this.isAdmin === false ||
        this.isFeatureEnabled === true ||
        this.userActed === true ||
        this.isBannerPermanentlyDismissed === true
      ) {
        return false;
      }

      // Show banner (even if reminded once - gives one more chance)
      return true;
    },
  },
  async mounted() {
    // Get job edit UI meta for admin check
    const jobEditUiMeta = loadJsonData("jobEditUiMeta") || {};
    this.isAdmin =
      jobEditUiMeta.isAdmin === true || jobEditUiMeta.isAdmin === "true";

    // Check if feature is already enabled
    try {
      this.isFeatureEnabled = await getFeatureEnabled(
        "earlyAccessJobConditional",
      );
    } catch (error) {
      console.warn("[BANNER] Failed to check feature flag:", error);
      this.isFeatureEnabled = false;
    }

    // Check cookies
    const remindedCookie = this.$cookies.get(COOKIE_REMINDED);
    const permanentlyDismissedCookie = this.$cookies.get(
      COOKIE_PERMANENTLY_DISMISSED,
    );

    this.isBannerReminded = remindedCookie === "true";
    this.isBannerPermanentlyDismissed = permanentlyDismissedCookie === "true";
  },
  methods: {
    handleTryNow() {
      if (this.eventBus) {
        this.eventBus.emit("conditional-early-access:try-now");
      } else {
        console.error("[BANNER] EventBus not available");
      }
    },
    handleRemindMeLater() {
      this.$cookies.set(
        COOKIE_REMINDED,
        "true",
        "1y",
        "/",
        "",
        false,
        "Strict",
      );
      this.isBannerReminded = true;
      this.userActed = true;

      Notification.notify({
        type: "info",
        title: "",
        content: this.$t("earlyAccessRemindLaterToast"),
      });
    },
    handleClose() {
      this.$cookies.set(
        COOKIE_PERMANENTLY_DISMISSED,
        "true",
        "1y",
        "/",
        "",
        false,
        "Strict",
      );
      this.$cookies.remove(COOKIE_REMINDED, "/");
      this.isBannerPermanentlyDismissed = true;

      Notification.notify({
        type: "info",
        title: "",
        content: this.$t("earlyAccessGoToSettings"),
      });
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
