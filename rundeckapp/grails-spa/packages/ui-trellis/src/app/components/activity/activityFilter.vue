<template>
  <span>
    <dropdown
      v-if="query.recentFilter !== '-' && displayOpts.showRecentFilter"
      style="vertical-align: inherit"
      data-test-id="dropdown"
    >
      <span
        class="dropdown-toggle text-info cursor-pointer mr-2"
        data-test-id="dropdown-toggle"
      >
        {{ $t(`period.label.${period.name}`) }}
        <span class="caret"></span>
      </span>
      <template #dropdown>
        <li
          v-for="perobj in periods"
          :key="perobj.name"
          data-test-id="dropdown-item"
        >
          <a
            role="button"
            data-test-id="period-option"
            @click="changePeriod(perobj)"
          >
            {{ $t(`period.label.${perobj.name}`) }}
            <span v-if="period.name === perobj.name">√</span>
          </a>
        </li>
      </template>
    </dropdown>
    <btn
      v-if="displayOpts.showFilter"
      v-tooltip="hasQuery ? $t('Click to edit Search Query') : ''"
      size="xs"
      :class="hasQuery ? 'btn-queried btn-info' : 'btn-default'"
      data-test-id="filter-button"
      @click="filterOpen = true"
    >
      <span v-if="hasQuery" class="query-params-summary">
        <ul class="list-inline">
          <li
            v-for="qname in queryParamsList"
            :key="qname"
            data-test-id="query-param"
          >
            {{ $t(`jobquery.title.${qname}`) }}:
            <code class="queryval">{{ query[qname] }}</code>
          </li>
        </ul>
      </span>
      <span v-else>{{ $t("search.ellipsis") }}</span>
    </btn>
    <saved-filters
      v-if="modelValue && displayOpts.showSavedFilters"
      :query="modelValue"
      :has-query="hasQuery"
      :event-bus="eventBus"
      data-test-id="saved-filters"
      @select_filter="selectFilter($event)"
    ></saved-filters>
    <modal
      id="activityFilter"
      v-model="filterOpen"
      :title="$t('Search Activity')"
      size="lg"
      append-to-body
      data-test-id="modal"
      @hide="closing"
    >
      <div>
        <div class="base-filters">
          <div class="row">
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="jobIdFilter" class="sr-only">
                  {{ $t("jobquery.title.jobFilter") }}
                </label>
                <input
                  v-model="query.jobFilter"
                  type="text"
                  name="jobFilter"
                  autofocus="true"
                  class="form-control"
                  :placeholder="$t('jobquery.title.jobFilter')"
                  data-test-id="job-filter"
                />
              </div>
              <div v-if="query.jobIdFilter" class="form-group">
                <label for="jobIdFilter" class="sr-only">
                  {{ $t("jobquery.title.jobIdFilter") }}
                </label>
                <input
                  v-model="query.jobIdFilter"
                  type="text"
                  name="jobIdFilter"
                  class="form-control"
                  :placeholder="$t('jobquery.title.jobIdFilter')"
                  data-test-id="job-id-filter"
                />
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="userFilter" class="sr-only">
                  {{ $t("jobquery.title.userFilter") }}
                </label>
                <input
                  v-model="query.userFilter"
                  type="text"
                  name="userFilter"
                  class="form-control"
                  :placeholder="$t('jobquery.title.userFilter')"
                  data-test-id="user-filter"
                />
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="execnodeFilter" class="sr-only">
                  {{ $t("jobquery.title.filter") }}
                </label>
                <input
                  v-model="query.execnodeFilter"
                  type="text"
                  name="execnodeFilter"
                  class="form-control"
                  :placeholder="$t('jobquery.title.filter')"
                  data-test-id="execnode-filter"
                />
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="titleFilter" class="sr-only">
                  {{ $t("jobquery.title.titleFilter") }}
                </label>
                <input
                  v-model="query.titleFilter"
                  type="text"
                  name="titleFilter"
                  class="form-control"
                  :placeholder="$t('jobquery.title.titleFilter')"
                  data-test-id="title-filter"
                />
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="statFilter" class="sr-only">
                  {{ $t("jobquery.title.statFilter") }}
                </label>
                <select
                  v-model="query.statFilter"
                  name="statFilter"
                  noSelection="['': 'Any']"
                  valueMessagePrefix="status.label"
                  class="form-control"
                  data-test-id="stat-filter"
                >
                  <option value>Any</option>
                  <option>succeed</option>
                  <option>fail</option>
                  <option>cancel</option>
                  <option>missed</option>
                </select>
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <label for="recentFilter" class="sr-only">
                  {{ $t("jobquery.title.recentFilter") }}
                </label>
                <span class="radiolist">
                  <select
                    v-model="query.recentFilter"
                    name="recentFilter"
                    class="form-control"
                    data-test-id="recent-filter"
                  >
                    <option value>Any Time</option>
                    <option
                      v-for="(key, val) in recentDateFilters"
                      :key="key"
                      :value="val"
                    >
                      {{ key }}
                    </option>
                    <option value="-">Other...</option>
                  </select>
                </span>
              </div>
            </div>
          </div>
        </div>
        <div
          v-if="query.recentFilter === '-'"
          class="date-filters panel panel-default"
          data-test-id="date-filters"
        >
          <div class="panel-body form-horizontal">
            <div
              v-for="df in DateFilters"
              :key="df.name"
              class="container-fluid"
              data-test-id="date-filter-container"
            >
              <date-filter v-model="df.filter">{{
                $t("jobquery.title." + df.name)
              }}</date-filter>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <btn @click="filterOpen = false">{{ $t("cancel") }}</btn>
        <btn
          type="primary"
          class="btn btn-primary"
          data-testid="searchfilter"
          @click="search"
        >
          {{ $t("search") }}
        </btn>

        >
        <btn type="default" class="btn-default pull-right" @click="saveFilter">
          <i class="glyphicon glyphicon-plus"></i>
          {{ $t("Save as a Filter...") }}
        </btn>
      </template>
    </modal>
  </span>
</template>
<script>
import { defineComponent } from "vue";
import DateTimePicker from "./dateTimePicker.vue";
import DateFilter from "./dateFilter.vue";
import SavedFilters from "./savedFilters.vue";

export default defineComponent({
  name: "ActivityFilter",
  components: {
    DateTimePicker,
    DateFilter,
    SavedFilters,
  },
  props: {
    eventBus: {
      type: Object,
      required: true,
    },
    modelValue: {
      type: Object,
      required: true,
    },
    opts: {
      type: Object,
      required: true,
    },
  },
  emit: ["update:modelValue"],
  data() {
    return {
      displayOpts: {
        showRecentFilter: true,
        showFilter: true,
        showSavedFilters: true,
      },
      filterOpen: false,
      DateQueryNames: [
        "startafterFilter",
        "startbeforeFilter",
        "endafterFilter",
        "endbeforeFilter",
      ],
      QueryNames: [
        "jobFilter",
        "jobIdFilter",
        "userFilter",
        "execnodeFilter",
        "titleFilter",
        "statFilter",
        "startafterFilter",
        "startbeforeFilter",
        "endafterFilter",
        "endbeforeFilter",
      ],
      DateFilters: [
        {
          name: "startafterFilter",
          filter: {
            enabled: false,
            datetime: "",
          },
        },
        {
          name: "startbeforeFilter",
          filter: {
            enabled: false,
            datetime: "",
          },
        },
        {
          name: "endafterFilter",
          filter: {
            enabled: false,
            datetime: "",
          },
        },
        {
          name: "endbeforeFilter",
          filter: {
            enabled: false,
            datetime: "",
          },
        },
      ],
      query: {
        jobFilter: "",
        jobIdFilter: "",
        userFilter: "",
        execnodeFilter: "",
        titleFilter: "",
        statFilter: "",
        recentFilter: "",
        startafterFilter: "",
        startbeforeFilter: "",
        endafterFilter: "",
        endbeforeFilter: "",
      },
      hasQuery: false,
      showDateFilters: false,
      recentDateFilters: {
        "1h": "1 Hour",
        "1d": "1 Day",
        "1w": "1 Week",
        "1m": "1 Month",
      },
      didSearch: false,
      period: { name: "All", params: {} },
      periods: [
        { name: "All", params: { recentFilter: "" } },
        { name: "Hour", params: { recentFilter: "1h" } },
        { name: "Day", params: { recentFilter: "1d" } },
        { name: "Week", params: { recentFilter: "1w" } },
        { name: "Month", params: { recentFilter: "1m" } },
      ],
    };
  },
  computed: {
    queryParamsList() {
      return this.QueryNames.filter((s) => !!this.query[s]);
    },
  },
  watch: {
    query: {
      handler(newValue, oldValue) {
        this.updateSelectedPeriod();
      },
    },
    modelValue: {
      handler(newValue, oldValue) {
        this.reset();
      },
      deep: true,
    },
    DateFilters: {
      handler(newValue, oldValue) {
        newValue.forEach((element) => {
          if (element.filter.enabled) {
            this.query["do" + element.name] = "true";
            this.query[element.name] = element.filter.datetime;
          } else {
            this.query["do" + element.name] = "false";
            this.query[element.name] = "";
          }
        });
      },
      deep: true,
    },
  },
  mounted() {
    Object.assign(this.displayOpts, this.opts);
    this.reset();
  },
  methods: {
    checkQueryIsPresent() {
      const isquery = this.QueryNames.findIndex((q) => this.query[q]) >= 0;
      this.hasQuery = isquery;
    },
    updated() {
      this.$emit("update:modelValue", this.query);
    },
    search() {
      this.checkQueryIsPresent();
      this.query.filterName = "";
      this.didSearch = true;
      this.filterOpen = false;
      this.updated();
    },
    selectFilter(filter) {
      this.QueryNames.forEach((v) => (this.query[v] = filter.query[v]));
      this.DateQueryNames.forEach((v) => {
        if (filter.query[v]) {
          this.query["do" + v] = "true";
        }
      });
      if (filter.query.recentFilter) {
        this.query.recentFilter = filter.query.recentFilter;
      }
      this.query.filterName = filter.filterName;
      this.checkQueryIsPresent();
      this.updateSelectedPeriod();
      this.updated();
    },
    cancel() {
      this.reset();
      this.filterOpen = false;
    },
    reset() {
      this.query = Object.assign({}, this.modelValue);
      this.DateFilters.forEach((element) => {
        element.filter.datetime = this.query[element.name];
        element.filter.enabled = !!element.filter.datetime;
      });
      this.checkQueryIsPresent();
    },
    closing() {
      if (this.didSearch) {
        this.didSearch = false;
      } else {
        this.reset();
      }
    },
    saveFilter() {
      this.search();
      // I added a timeout to the popping of the save modal
      // this allows for the filter to be applied before
      // opening the save modal which resolves an issues we experienced
      // where the saving of a filter (giving it a name) didn't actually
      // save the filter itself, just the name
      setTimeout(() => {
        this.eventBus.emit("invoke-save-filter");
        this.didSearch = true;
        this.filterOpen = false;
      }, 500);
    },
    changePeriod(period) {
      this.period = period;
      this.query.recentFilter = period.params.recentFilter;
      this.updated();
    },
    updateSelectedPeriod() {
      if (
        this.query.recentFilter &&
        this.period &&
        this.query.recentFilter !== this.period.params.recentFilter
      ) {
        const p = this.periods.find(
          (v) => v.params.recentFilter === this.query.recentFilter,
        );
        if (p && p !== this.period) {
          this.period = p;
        }
      }
    },
  },
});
</script>
<style lang="scss" scoped>
.query-params-summary {
  ul.list-inline {
    display: inline-block;
    margin: 0;
  }
}

.btn-queried {
  border-style: dotted;
}

.btn-primary {
  color: var(--font-fill-color);
  background-color: var(--primary-color);

  &:hover {
    color: var(--font-fill-color);
    background-color: var(--primary-states-color);
  }
}
</style>
