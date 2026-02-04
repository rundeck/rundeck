<template>
  <div v-if="isOpen" class="settings-modal-overlay" @click.self="close">
    <div class="settings-modal">
      <button class="settings-modal__close" @click="close">
        <i class="fas fa-times"></i>
      </button>

      <div class="settings-modal__content">
        <nav class="settings-modal__tabs">
          <button
            class="settings-modal__tab"
            :class="{ 'settings-modal__tab--active': currentTab === 'theme' }"
            @click="changeTab('theme')"
          >
            {{ $t("settings.tabs.theme") }}
          </button>
          <button
            class="settings-modal__tab"
            :class="{ 'settings-modal__tab--active': currentTab === 'ui-early-access' }"
            @click="changeTab('ui-early-access')"
          >
            {{ $t("settings.tabs.uiEarlyAccess") }}
          </button>
        </nav>

        <div class="settings-modal__panel">
          <div v-if="currentTab === 'theme'" class="settings-panel">
            <h2 class="settings-panel__title">{{ $t("settings.theme.title") }}</h2>
            <p class="settings-panel__description">
              {{ $t("settings.theme.description") }}
            </p>
            <div class="settings-panel__control">
              <select v-model="theme" class="form-control">
                <option v-for="themeOpt in themes" :key="themeOpt" :value="themeOpt">
                  {{ $t(`settings.theme.options.${themeOpt}`) }}
                </option>
              </select>
            </div>
          </div>

          <div v-if="currentTab === 'ui-early-access'" class="settings-panel">
            <h2 class="settings-panel__title">{{ $t("settings.uiEarlyAccess.title") }}</h2>
            <p class="settings-panel__description">
              {{ $t("settings.uiEarlyAccess.description") }}
            </p>

            <div class="settings-panel__toggle">
              <label class="settings-toggle">
                <span class="settings-toggle__label">{{ $t("settings.uiEarlyAccess.enableLabel") }}</span>
                <input
                  type="checkbox"
                  v-model="nextUiEnabled"
                  class="settings-toggle__input"
                  @change="handleNextUiToggle"
                />
                <span class="settings-toggle__switch"></span>
              </label>
            </div>

            <p class="settings-panel__description">
              {{ $t("settings.uiEarlyAccess.feature1") }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../../library";
import type {
  SettingsTab,
  ThemeOption,
  SettingsModalData,
} from "./SettingsModalTypes";
import { getPageUiMeta } from "./services/pageUiMetaService";

const COOKIE_NAME = "nextUi";
const LEARN_MORE_URL = "https://docs.rundeck.com";
const FEEDBACK_URL = "https://feedback.rundeck.com";

export default defineComponent({
  name: "SettingsModal",
  props: {
    isOpen: {
      type: Boolean,
      required: true,
    },
    activeTab: {
      type: String as () => SettingsTab,
      required: true,
    },
  },
  emits: ["close", "change-tab"],
  data(): SettingsModalData {
    const rundeckContext = getRundeckContext();
    const themeStore = rundeckContext?.rootStore?.theme;
    const pageUiMeta = getPageUiMeta();

    return {
      currentTab: this.activeTab,
      themes: ["system", "light", "dark"] as ThemeOption[],
      theme: (themeStore?.userPreferences?.theme ||
        "system") as ThemeOption,
      themeStore,
      nextUiEnabled: pageUiMeta.isNextUiPage,
      isLoading: false,
      learnMoreUrl: LEARN_MORE_URL,
      feedbackUrl: FEEDBACK_URL,
    };
  },
  watch: {
    activeTab(newVal: SettingsTab) {
      this.currentTab = newVal;
    },
    theme(newVal: ThemeOption) {
      if (this.themeStore) {
        this.themeStore.setUserTheme(newVal);
      }
    },
  },
  methods: {
    close(): void {
      this.$emit("close");
    },
    changeTab(tab: SettingsTab): void {
      this.currentTab = tab;
      this.$emit("change-tab", tab);
    },
    handleNextUiToggle(): void {
      if (this.nextUiEnabled) {
        this.$cookies.set(COOKIE_NAME, "true", "1y", "/", "", false, "Strict");
      } else {
        this.$cookies.set(
          COOKIE_NAME,
          "false",
          "1y",
          "/",
          "",
          false,
          "Strict",
        );
      }
      this.isLoading = true;
      window.location.reload();
    },
  },
});
</script>

<style scoped lang="scss">
.settings-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.settings-modal {
  background: var(--background-color, #fff);
  border-radius: var(--radii-md, 6px);
  box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.3);
  width: 600px;
  max-width: 90vw;
  max-height: 80vh;
  overflow: hidden;
  position: relative;
}

.settings-modal__close {
  position: absolute;
  top: var(--spacing-3, 12px);
  right: var(--spacing-3, 12px);
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--font-color);
  font-size: 16px;
  padding: var(--spacing-1, 4px);
  z-index: 1;

  &:hover {
    color: var(--colors-gray-600);
  }
}

.settings-modal__content {
  display: flex;
  min-height: 400px;
}

.settings-modal__tabs {
  display: flex;
  flex-direction: column;
  width: 180px;
  background-color: var(--background-color-lvl2, #f5f5f5);
  border-right: 1px solid var(--colors-gray-200, #e5e7eb);
  padding: var(--spacing-4, 16px) 0;
}

.settings-modal__tab {
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  background: transparent;
  border: none;
  border-left: 3px solid transparent;
  text-align: left;
  cursor: pointer;
  color: var(--font-color);
  font-size: 14px;

  &:hover {
    background-color: var(--background-color-accent-lvl2);
  }

  &--active {
    border-left-color: var(--colors-blue-500, #3b82f6);
    background-color: var(--background-color, #fff);
    font-weight: 600;
  }
}

.settings-modal__panel {
  flex: 1;
  padding: var(--spacing-6, 24px);
  overflow-y: auto;
}

.settings-panel__title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 var(--spacing-2, 8px) 0;
  color: var(--font-color);
}

.settings-panel__description {
  font-size: 14px;
  color: var(--font-color-muted, #6b7280);
  margin: 0 0 var(--spacing-4, 16px) 0;
  line-height: 1.5;
}

.settings-panel__control {
  margin-bottom: var(--spacing-4, 16px);

  select {
    width: 100%;
    padding: var(--spacing-2, 8px) var(--spacing-3, 12px);
    border: 1px solid var(--colors-gray-300, #d1d5db);
    border-radius: var(--radii-md, 6px);
    font-size: 14px;
    background-color: var(--background-color, #fff);
    color: var(--font-color);
  }
}

.settings-panel__toggle {
  margin-bottom: var(--spacing-4, 16px);
}

.settings-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.settings-toggle__label {
  font-size: 14px;
  color: var(--font-color);
}

.settings-toggle__input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.settings-toggle__switch {
  position: relative;
  width: 44px;
  height: 24px;
  background-color: var(--colors-gray-300, #d1d5db);
  border-radius: 12px;
  transition: background-color 0.2s;

  &::after {
    content: "";
    position: absolute;
    top: 2px;
    left: 2px;
    width: 20px;
    height: 20px;
    background-color: white;
    border-radius: 50%;
    transition: transform 0.2s;
  }
}

.settings-toggle__input:checked + .settings-toggle__switch {
  background-color: var(--colors-blue-500, #3b82f6);

  &::after {
    transform: translateX(20px);
  }
}

.settings-panel__features {
  margin-bottom: var(--spacing-4, 16px);
}

.settings-panel__features-title {
  font-size: 14px;
  color: var(--font-color);
  margin: 0 0 var(--spacing-2, 8px) 0;
}

.settings-panel__features-list {
  margin: 0;
  padding-left: var(--spacing-4, 16px);
  font-size: 14px;
  color: var(--font-color);

  li {
    margin-bottom: var(--spacing-1, 4px);
  }
}

.settings-panel__link {
  color: var(--colors-blue-600, #2563eb);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.settings-panel__feedback-link {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  color: var(--colors-blue-600, #2563eb);
  text-decoration: none;
  font-size: 14px;

  &:hover {
    text-decoration: underline;
  }

  i {
    font-size: 12px;
  }
}
</style>
