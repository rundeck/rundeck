<template>
  <div class="card">
    <div id="project-list" class="card-content">
      <HomeSearchBar
          :placeholder="$t('page.home.search.projects.input.placeholder')"
          v-model="search"
          @on-focus="hideResults"
          @on-enter="handleSearch"
          @on-blur="handleSearch"
      />
      <p class="alert alert-info" v-if="showSearchResults && loadedProjectNames">
        <span :class="{ 'text-white': searchedProjectsCount>0, 'text-warning': searchedProjectsCount<1 }">
          {{ $tc('page.home.search.project.title', searchedProjectsCount) }}
        </span>
      </p>

      <div class="project_list_header">
        <div class="row row-border-top p-4">
          <div class="col-sm-6 col-md-8">
            <p class="text-lg">
              {{ $t('home.table.projects') }}
            </p>
          </div>
          <div class="col-sm-6 col-md-2">
            <p class="text-lg">
              {{ $t('home.table.activity') }}
            </p>
          </div>
          <div class="col-sm-12 col-md-2 col-last">
            <p class="text-lg">
              {{ $t('home.table.actions') }}
            </p>
          </div>
        </div>
      </div>

      <HomeBrowser :projects="filteredProjects" />
    </div>
  </div>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue'
import HomeSearchBar from "./HomeSearchBar.vue";
import HomeBrowser from "./HomeBrowser.vue";
import {Project} from "@/app/components/home/types/projectTypes";

export default defineComponent({
  name: "HomeCardList",
  components: {
    HomeSearchBar,
    HomeBrowser
  },
  props: {
    loadedProjectNames: {
      type: Boolean,
      default: false
    },
    projects: {
      type: Array as PropType<Project[]>,
      default: () => []
    }
  },
  data() {
    return {
      search: "",
      filteredProjects: [],
      showSearchResults: false
    }
  },
  computed: {
    searchedProjectsCount(): number {
      return this.filteredProjects?.length || 0
    },
  },
  methods: {
    handleSearch() {
      let regex;
      if (this.search.charAt(0) === '/' && this.search.charAt(this.search.length - 1) === '/') {
        regex = new RegExp(this.search.substring(1, this.search.length - 1),'i');
      }else{
        //simple match which is case-insensitive, escaping special regex chars
        regex = new RegExp(this.search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'),'i');
      }
      this.filteredProjects = this.projects.filter((project) => {
        return project.name.match(regex) || project.label && project.label.match(regex);
      });

      this.showSearchResults = true;
    },
    hideResults() {
      this.showSearchResults = false;
    }
  },
  watch: {
    projects(newVal) {
      if(this.search === "") {
        this.filteredProjects = newVal
      }
    }
  }
})
</script>

<style scoped lang="scss">
.project_list_header {
  .text-lg {
    margin: 0;
  }
}

.project_list_item_link {
  p {
    margin: 0;
    + text-secondary {
      margin-top: var(--spacing-1);
    }
  }
}

.project_list_item_link:not(:has(> :nth-child(4))) > p {
  display: inline;
}

.has_tooltip {
  margin-right: var(--spacing-2);
}
</style>