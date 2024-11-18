import { defineComponent, markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import HomeView from "../../components/home/HomeView.vue";
import HomeHeader from "../../components/home/HomeHeader.vue";
import SVGInject from '@iconfu/svg-inject';

// @ts-ignore
window.SVGInject = SVGInject;

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
                      <HomeView v-bind="itemData"  app-title="" build-ident="" help-link-url="" logo-image=""/>
                    `,
        }),
      ),
    },
  ]);
}
window.addEventListener("DOMContentLoaded", init);
