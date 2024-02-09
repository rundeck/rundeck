<template>
  <div class="card">
    <div id="project-list" class="card-content">
      <div>
        <HomeSearchBar
          :placeholder="$t('page.home.search.projects.input.placeholder')"
          v-model="search"
          @on-focus="hideResults"
          @on-enter="handleSearch()"
          @on-blur="handleSearch()"
        />
        <ui-socket location="table-header" section="home" />
        <p
          data-test="searchResults"
          class="alert alert-info"
          v-if="showSearchResults && loadedProjectNames"
        >
          <span
            :class="{
              'text-white': searchResultsCount > 0,
              'text-warning': searchResultsCount < 1,
            }"
          >
            {{ $tc("page.home.search.project.title", searchResultsCount) }}
          </span>
        </p>
        <offset-pagination
          :pagination="pagination"
          @change="changePageOffset"
          :showPrefix="true"
        ></offset-pagination>
      </div>

      <div class="project_list_header">
        <div class="row row-border-top p-4">
          <div class="col-sm-6 col-md-8">
            <p class="text-lg">
              {{ $t("home.table.projects") }}
            </p>
          </div>
          <div class="col-sm-6 col-md-2">
            <p class="text-lg">
              {{ $t("home.table.activity") }}
            </p>
          </div>
          <div class="col-sm-12 col-md-2 col-last">
            <p class="text-lg">
              {{ $t("home.table.actions") }}
            </p>
          </div>
        </div>
      </div>
      <HomeBrowser
        v-if="searchResultsCount > 0"
        :key="searchResultsCount"
        :projects="resultsPage"
        :allProjects="projects"
        :loaded="loadedProjectNames"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import HomeSearchBar from "./HomeSearchBar.vue";
import HomeBrowser from "./HomeBrowser.vue";
import { Project } from "@/app/components/home/types/projectTypes";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { getRundeckContext } from "@/library";
import OffsetPagination from "@/library/components/utils/OffsetPagination.vue";

export default defineComponent({
  name: "HomeCardList",
  components: {
    UiSocket,
    HomeSearchBar,
    HomeBrowser,
    OffsetPagination,
  },
  props: {
    loadedProjectNames: {
      type: Boolean,
      default: false,
    },
    projects: {
      type: Array as PropType<Project[]>,
      default: () => [],
    },
    pagingMax: {
      type: Number,
      default: 30,
    },
  },
  data() {
    return {
      search: "",
      filteredProjects: [] as Project[],
      favoriteProjectNames: [] as Project[],
      resultsPage: [] as Project[],
      showSearchResults: false,
      filterFavoritesOnly: false,
      eventBus: getRundeckContext().eventBus,
      pagination: {
        offset: 0,
        max: this.pagingMax,
        total: -1,
      },
    };
  },
  computed: {
    searchResultsCount(): number {
      return this.filteredProjects?.length || 0;
    },
    favoriteProjects(): Project[] {
      if (!this.projects || !this.favoriteProjectNames) {
        return [];
      }

      return this.projects.filter((project: Project) =>
        this.favoriteProjectNames.includes(project.name),
      );
    },
  },
  methods: {
    // filters the projects based on the search input and if the user has selected to only show favorites
    // search input can either be a string or a regex, and the search is case-insensitive
    // updates the pagination to reflect the total number of projects and sets flag to show search results message
    // if the search input is not empty
    handleSearch(resetPagination: boolean = true) {
      if (resetPagination) {
        this.pagination.offset = 0;
      }

      let regex: RegExp;
      if (
        this.search.charAt(0) === "/" &&
        this.search.charAt(this.search.length - 1) === "/"
      ) {
        regex = new RegExp(
          this.search.substring(1, this.search.length - 1),
          "i",
        );
      } else {
        //simple match which is case-insensitive, escaping special regex chars
        regex = new RegExp(
          this.search.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"),
          "i",
        );
      }

      let searchSet = this.filterFavoritesOnly
        ? this.favoriteProjects
        : this.projects;
      this.filteredProjects = searchSet.filter((project: Project) => {
        return (
          project.name.match(regex) ||
          (project.label && project.label.match(regex))
        );
      });

      this.resultsPage = this.paginateResults(this.filteredProjects);

      // update the pagination to reflect the total number of projects
      this.pagination.total = this.searchResultsCount;

      this.showSearchResults = this.search !== "" || this.filterFavoritesOnly;
    },
    hideResults() {
      this.showSearchResults = false;
    },
    changePageOffset(offset: any) {
      this.pagination.offset = offset;
      this.handleSearch(false);
    },
    paginateResults(results = this.filteredProjects): Project[] {
      const offset = this.pagination.offset;
      const max = this.pagination.max;
      return results.slice(offset, offset + max);
    },
  },
  mounted() {
    this.eventBus.on(
      "project-favorites-toggle:status",
      ({ favsOnly, favoriteProjects }) => {
        this.filterFavoritesOnly = favsOnly;
        this.favoriteProjectNames = favoriteProjects;
        // eventBus might emit before projects are loaded
        if (this.projects) {
          this.handleSearch();
        }
      },
    );
  },
  watch: {
    projects: {
      immediate: true,
      deep: true,
      handler(newVal: Project[]) {
        if (newVal && newVal.length) {
          this.handleSearch();
          this.pagination.total = newVal.length;
        }
      },
    },
  },
});
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
