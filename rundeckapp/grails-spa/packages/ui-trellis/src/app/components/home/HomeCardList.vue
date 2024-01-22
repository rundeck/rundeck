<template>
  <div class="card">
    <div id="project-list" class="card-content">
      <div>
        <HomeSearchBar
            :placeholder="$t('page.home.search.projects.input.placeholder')"
            v-model="search"
            @on-focus="hideResults"
            @on-enter="handleSearch"
            @on-blur="handleSearch"
        />
        <ui-socket location="table-header" section="home" />
        <p data-test="searchResults" class="alert alert-info" v-if="showSearchResults && loadedProjectNames">
          <span :class="{ 'text-white': searchResultsCount>0, 'text-warning': searchResultsCount<1 }">
            {{ $tc('page.home.search.project.title', searchResultsCount) }}
          </span>
        </p>
      </div>

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
import UiSocket from "@/library/components/utils/UiSocket.vue";
import {getRundeckContext} from "@/library";

export default defineComponent({
  name: "HomeCardList",
  components: {
    UiSocket,
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
      favoriteProjectNames: [],
      showSearchResults: false,
      filterFavoritesOnly: false,
      eventBus: getRundeckContext().eventBus
    }
  },
  computed: {
    searchResultsCount(): number {
      return this.filteredProjects?.length || 0
    },
    favoriteProjects(): Project[] {
      if (!this.projects || !this.favoriteProjectNames) {
        return []
      }

      return this.projects.filter((project) => this.favoriteProjectNames.includes(project.name))
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

      if(this.filterFavoritesOnly) {
        this.filteredProjects = this.favoriteProjects.filter((project) => {
          return project.name.match(regex) || project.label && project.label.match(regex);
        });
      } else {
        this.filteredProjects = this.projects.filter((project) => {
          return project.name.match(regex) || project.label && project.label.match(regex);
        });
      }

      if (this.search !== "") {
        this.showSearchResults = true;
      }
    },
    hideResults() {
      this.showSearchResults = false;
    },
  },
  mounted() {
    this.eventBus.on("home-card-list:toggleFavorites",  ({favsOnly, favoriteProjects}) => {
      this.filterFavoritesOnly = favsOnly;
      this.favoriteProjectNames = favoriteProjects;
      // eventBus might emit before projects are loaded
      if(this.projects){
        this.handleSearch();
      }
    });
  },
  watch: {
    projects(newVal) {
      if(newVal.length) {
        this.handleSearch();
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