import { defineComponent, markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import HomeView from "../../components/home/HomeView.vue";
import HomeHeader from "../../components/home/HomeHeader.vue";

// @ts-ignore
window.SVGInject = require("@iconfu/svg-inject");

const rundeckContext = getRundeckContext();
function init() {
  rundeckContext.rootStore.ui.addItems([
    {
      section: "home",
      location: "header",
      visible: true,
      widget: markRaw(
        defineComponent({
          components: { HomeHeader },
          props: ["itemData"],
          data() {
            return {
              project: rundeckContext.projectName,
            };
          },
          template: `
                      <HomeHeader :createProjectAllowed="itemData.createProjectAllowed" :projectCount="itemData.projectCount" />
                    `,
        }),
      ),
    },
    {
      section: "home",
      location: "list",
      visible: true,
      widget: markRaw(
        defineComponent({
          name: "HomeProjectView",
          components: { HomeView },
          props: ["itemData"],
          data() {
            return {};
          },
          template: `
                      <HomeView v-bind="itemData" />
                    `,
        }),
      ),
    },
  ]);
}
window.addEventListener("DOMContentLoaded", init);
