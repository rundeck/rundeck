import { config } from "@vue/test-utils";
import { Btn, Dropdown, Modal } from "uiv";
import PrimeVue from "primevue/config";

config.global.components = {
  Btn,
  Dropdown,
  Modal,
};

config.global.plugins = [PrimeVue];

config.global.mocks = {
  $t: (msg: string) => msg,
};
