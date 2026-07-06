import { defineComponent, markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import type { LocalizedMessages } from "../../utilities/i18n";
import EditProjectFile from "./EditProjectFile.vue";

import messages from "./i18n";

const rundeckContext = getRundeckContext();

rundeckContext.rootStore.ui.addItems([
  {
    section: "edit-project-file",
    location: "main",
    visible: true,
    widget: markRaw(
      defineComponent({
        components: {
          EditProjectFile,
        },
        inject: ["addUiMessages"],
        props: {
          itemData: {
            type: Object,
            required: true,
          },
        },

        data() {
          return {
            filename: "",
            displayConfig: [] as string[],
            project: "",
            authAdmin: false,
          };
        },
        created() {
          (this.addUiMessages as (messages: LocalizedMessages) => Promise<void>)(
            messages as LocalizedMessages,
          );

          this.filename = this.itemData.filename;
          // code to handle displayConfig

          if (typeof this.itemData.displayConfig === "string") {
            this.displayConfig = this.itemData.displayConfig
              .replace(/^\[|]$/g, "")
              .split(", ");
          }

          this.project = this.itemData.project;
          this.authAdmin = Boolean(this.itemData.authAdmin);
        },
        template: `
              <edit-project-file :filename="filename" :display-config="displayConfig" :project="project"
                                 :auth-admin="authAdmin"/>\`
            `,
      }),
    ),
  },
]);
