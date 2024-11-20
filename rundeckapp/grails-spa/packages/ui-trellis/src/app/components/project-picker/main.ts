import { createApp } from "vue";

import { getRundeckContext, url } from "../../../library";

import ProjectPicker from "../../../library/components/widgets/project-select/ProjectSelectButton.vue";
import { Project } from "../../../library/stores/Projects";

const rootStore = getRundeckContext().rootStore;

window.addEventListener("DOMContentLoaded", init);

function init() {
  const el = document.getElementById("projectPicker") as HTMLElement;

  if (!el) return;

  const component = createApp({
    name: "ProjectPickerApp",
    components: { ProjectPicker },
    provide: { rootStore },
    data() { return { rootStore }},
    template: `<ProjectPicker :project-store="rootStore.projects" projectLabel="${el.dataset.projectLabel}"/>`,
  });
  component.mount(el);
}
