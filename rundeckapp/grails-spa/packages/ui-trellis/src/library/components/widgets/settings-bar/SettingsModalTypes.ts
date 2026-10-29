import type { RundeckContext } from "../../../interfaces/rundeckWindow";
import type { Theme } from "../../../stores/Theme";

export type SettingsTab = "theme" | "ui-early-access";

export type ThemeOption = Theme;

export interface SettingsModalData {
  isOpen: boolean;
  currentTab: SettingsTab;
  themes: ThemeOption[];
  theme: ThemeOption;
  themeStore: RundeckContext["rootStore"]["theme"] | undefined;
  nextUiEnabled: boolean;
}
