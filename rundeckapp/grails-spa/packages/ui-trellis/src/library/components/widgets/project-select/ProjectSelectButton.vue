<template>
  <button
    class="btn btn-default btn-simple btn-transparent project-select-btn"
    aria-describedby="projectPicker"
    aria-controls="projectPicker"
    :aria-expanded="open"
    @click="handleClick"
    @mousedown="(e) => e.preventDefault()"
  >
    <i
      class="fas project-select-btn__left-icon"
      :class="{ 'fa-box-open': projectLabel, 'fa-box': !projectLabel }"
    />
    <span class="project-select-btn__label">
      <template v-if="showDefaultLabel">
        {{ projectLabel || "Projects" }}
      </template>
      <template v-else> {{ multiSelectLabel }} </template>
    </span>
    <i
      class="fas project-select-btn__right-icon"
      :class="[`fa-chevron-${open ? 'up' : 'down'}`]"
    />
    <Popper v-if="open" :open="open" @close="close">
      <div
        id="projectPicker"
        class="card card--popover project-select-btn__popper"
      >
        <ProjectSelect
          :show-buttons="showDefaultLabel"
          :mode="mode"
          :selected-projects="selectedProjects"
          @update:selection="handleSelect"
        />
      </div>
    </Popper>
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import Popper from "../../utility-bar/Popper.vue";

import ProjectSelect from "./ProjectSelect.vue";

export default defineComponent({
  components: {
    Popper,
    ProjectSelect,
  },
  props: {
    projectLabel: { type: String },
    showDefaultLabel: {
      type: Boolean,
      default: true,
    },
    mode: {
      type: String,
      default: "single",
      validator: (v: string) => ["single", "multi"].includes(v),
      description:
        "selection mode. Single renders a link that will take user to project page, while multiple won't.",
    },
    totalNumberOfProjects: {
      type: Number,
      default: 0,
      description:
        "this prop is only relevant when mode is multi, as it's used for calculating the button label",
    },
  },
  emits: ["update:selectedProjects"],
  data: () => ({
    open: false,
    selectedProjects: [],
  }),
  computed: {
    areAllProjectsSelected() {
      if (this.mode === "single" || this.selectedProjects.length === 0) {
        return false;
      }
      return (
        this.selectedProjects.length > 0 &&
        this.totalNumberOfProjects === this.selectedProjects.length
      );
    },
    multiSelectLabel() {
      if (this.showDefaultLabel || this.mode === "single") {
        return "";
      }
      if (this.selectedProjects.length === 0) {
        return this.$t("job.filter.project.none.selected");
      } else if (this.areAllProjectsSelected) {
        return this.$t("job.filter.project.all.selected", {
          n: this.totalNumberOfProjects,
        });
      } else if (this.selectedProjects.length <= 4) {
        return this.selectedProjects.join(", ");
      }
      return this.$t("job.filter.project.some.selected", {
        n: this.selectedProjects.length,
      });
    },
  },
  mounted() {
    window.addEventListener("keydown", this.handleEscape);
  },
  unmounted() {
    window.removeEventListener("keydown", this.handleEscape);
  },
  methods: {
    handleEscape(e) {
      if (e.key === "Escape" && this.open) {
        this.close();
      }
    },
    close() {
      this.open = false;
    },
    handleClick() {
      this.open = !this.open;
    },
    handleSelect(arrayOfNames: string[]) {
      if (
        arrayOfNames.length > 0 &&
        arrayOfNames.length < this.totalNumberOfProjects
      ) {
        arrayOfNames.forEach((projectName: string) => {
          if (this.selectedProjects.includes(projectName)) {
            this.selectedProjects = this.selectedProjects.filter(
              (selectedProject: string) => selectedProject !== projectName,
            );
          } else {
            this.selectedProjects.push(projectName);
          }
        });
      } else {
        this.selectedProjects = arrayOfNames;
      }

      this.$emit("update:selectedProjects", this.selectedProjects);
    },
  },
});
</script>

<style scoped lang="scss">
.project-select-btn {
  display: flex;
  align-content: center;
  align-items: center;
  width: 300px;
  span {
    margin: 0 5px;
  }

  .project-select-btn__right-icon {
    margin-left: auto;
  }

  .project-select-btn__label {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.project-select-btn__popper {
  width: 300px;
  height: 400px;
  overflow: hidden;
}
</style>
