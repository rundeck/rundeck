import { markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import MigrationWizard from "../../components/migwiz/MigrationWizard.vue";

const rundeckContext = getRundeckContext();

rundeckContext.rootStore.ui.addItems([
  {
    section: "migwiz",
    location: "main",
    visible: true,
    widget: markRaw(MigrationWizard),
  },
]);
