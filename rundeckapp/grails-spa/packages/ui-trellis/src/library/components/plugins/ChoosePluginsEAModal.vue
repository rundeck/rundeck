<template>
  <modal v-model="modalShown" :title="title || $t('plugin.choose.title')" size="lg">
    <p>{{ $t('searchForStep') }}</p>
    <p>{{ $t('learnMoreSearchPatterns') }}</p>
    <plugin-search
        v-if="showSearch"
        @search="filterLoadedServices"
    ></plugin-search>
    <select-button v-model="selectedService" :options="serviceOptions" optionLabel="name" optionValue="value"/>
    <p>{{ $t('nodeSteps') }}</p>
    <p>{{ $t('nodeStepsDescription') }}</p>
<!--    <p>Common node steps</p>-->
    <div class="placeholder">
      <skeleton height="1.25rem" width="1.25rem" shape="rectangle" />
      <skeleton />
    </div>
    <template v-if="Object.keys(groupedProviders).length > 0">
      <template v-for="(group, key) in groupedProviders">
        <div class="list-group">
          <div v-if="group.iconUrl" class="img-icon">
            <img :src="group.iconUrl" />
          </div>
           {{key}} ({{ group.providers.length }} {{ $t('plugins') }})
        </div>
      </template>
    </template>
    <template #footer>
      <btn data-testid="cancel-button" @click="$emit('cancel')">
        {{ $t("Cancel") }}
      </btn>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import { defineComponent } from "vue";
import PluginSearch from "@/library/components/plugins/PluginSearch.vue";
import { ServiceType } from "@/library/stores/Plugins";
import SelectButton from 'primevue/selectbutton';
import Skeleton from "primevue/skeleton";

const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginsEAModal",
  components: { PluginSearch, SelectButton, Skeleton },
  props: {
    title: {
      type: String,
      required: true,
    },
    services: {
      type: Array,
      required: true,
    },
    tabNames: {
      type: Array,
      required: false,
      default: () => [],
    },
    modelValue: {
      type: Boolean,
      required: false,
      default: false,
    },
    showSearch: {
      type: Boolean,
      default: false,
    },
    showDivider: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["cancel", "selected", "update:modelValue"],
  data() {
    return {
      loadedServices: [],
      loading: false,
      modalShown: false,
      searchQuery: "",
      selectedService: ServiceType.WorkflowNodeStep,
    };
  },
  computed: {
    serviceOptions() {
      return [
        {
          name: this.$t('nodeSteps'),
          value: ServiceType.WorkflowNodeStep,
        },
        {
          name: this.$t('workflowSteps'),
          value: ServiceType.WorkflowStep,
        }
      ];
    },
    filteredServices() {
      return this.loadedServices.map((service) => {
        const filteredProviders =
          service.providers?.filter((provider) =>
            this.matchesSearchQuery(provider),
          ) || [];
        return {
          ...service,
          providers: filteredProviders,
          dividerIndex: this.showDivider
            ? this.calculateDividerIndex(filteredProviders)
            : undefined,
        };
      });
    },
    groupedProviders(): Object {
      const tempMap = {};
      const activeService = this.filteredServices?.filter(service => service.service === this.selectedService)[0];
      if(activeService) {
        activeService.providers.map(provider => {
          if(provider.isHighlighted) {
            tempMap[provider.title] = {
              iconUrl: provider.iconUrl,
              title: provider.title,
              isHighlighted: true,
              providers: [provider],
            };
          } else {

            const splitChar = provider.title.includes(" / ") ? " / " : " ";
            const key = provider.title.split(splitChar)[0];

            const tempArray = tempMap[key]?.providers || [];
            const addOtherInfo = !!tempArray.length;
            console.log(key);


            tempArray.push(provider);


            tempMap[key] = {
              iconUrl: addOtherInfo ? provider.iconUrl : tempMap[key]?.iconUrl,
              title: addOtherInfo ? key : tempMap[key]?.title,
              isHighlighted: false,
              providers: tempArray,
            };
          }
        })
      }


      return tempMap;
    }
  },
  watch: {
    modelValue(val) {
      this.modalShown = val;
    },
    modalShown(val) {
      this.$emit("update:modelValue", val);
    },
  },
  async mounted() {
    this.loading = true;
    for (const service of this.services) {
      await context.rootStore.plugins.load(service);
    }
    this.loadedServices = this.services.map((service: string) => {
      const providers = context.rootStore.plugins.getServicePlugins(service);
      return {
        service,
        providers,
      };
    });
    this.loading = false;
    this.modalShown = this.modelValue;
  },
  methods: {
    filterLoadedServices(searchQuery: string) {
      this.searchQuery = searchQuery.toLowerCase();
    },
    matchesSearchQuery(provider) {
      if (!this.searchQuery) return true;
      const filterValue = this.searchQuery.split("=");
      const prop = filterValue.length > 1 ? filterValue[0] : "title";
      const value = filterValue.length > 1 ? filterValue[1] : filterValue[0];
      const propertyFilterValue = prop.split(":") || undefined;

      const filterByProps =
        propertyFilterValue && propertyFilterValue.length === 2;

      return filterByProps
        ? this.checkMatch(provider, propertyFilterValue[1], value)
        : this.checkMatch(provider, "title", value) ||
            this.checkMatch(provider, "name", value) ||
            this.checkMatch(provider, "description", value);
    },
    checkMatch(obj, field: string, val: string) {
      return obj[field] && val && obj[field].toLowerCase().indexOf(val) >= 0;
    },
    calculateDividerIndex(providers: any) {
      return providers.findIndex(
        (provider) => provider.isHighlighted === false,
      );
    },
  },
});
</script>

<style scoped lang="scss">
.placeholder {
  align-items: center;
  display: flex;
  gap: 0.5rem;
}

.img-icon {
  //height: var(--sizes-5);
  //width: var(--sizes-5);
  align-items: center;
  display: inline-flex;
  justify-content: center;
  height: 20px;
  width: 20px;

  img {
    height: auto;
    max-width: 80%;
  }
}
</style>
