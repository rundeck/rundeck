export type SettingsTab = "theme" | "ui-early-access";

export type ThemeOption = "system" | "light" | "dark";

export interface SettingsModalData {
  isOpen: boolean;
  currentTab: SettingsTab;
  themes: ThemeOption[];
  theme: ThemeOption;
  themeStore: any;
  nextUiEnabled: boolean;
  isLoading: boolean;
}
