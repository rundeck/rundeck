import { defineComponent, markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import HomeHeader from "../../components/home/HomeHeader.vue";

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
  ]);
}
window.addEventListener("DOMContentLoaded", init);
