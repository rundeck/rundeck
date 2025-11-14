import { markRaw } from "vue";
import AppUserMenu from "./user-menu/AppUserMenu.vue";
import { getRundeckContext } from "../../../library";

window.addEventListener("DOMContentLoaded", initUserMenu);

function initUserMenu() {
  const elm = document.getElementById("appUser") as HTMLElement;
  if (!elm) return;

  const rootStore = getRundeckContext().rootStore;

  rootStore.ui.addItems([
    {
      section: "mainbar-app-user-menu",
      location: "main",
      widget: markRaw(AppUserMenu),
      visible: true,
    },
  ]);
}
