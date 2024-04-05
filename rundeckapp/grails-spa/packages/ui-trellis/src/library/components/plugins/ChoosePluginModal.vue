<template>
  <modal v-model="modalShown" :title="title || $t('plugin.choose.title')">
    <slot></slot>
    <!-- todo: search -->

    <div v-if="loading">
      <i class="fas fa-spinner fa-spin"></i> {{ $t("loading.text") }}
    </div>
    <tabs v-else-if="loadedServices.length > 1" class="vue-tabs">
      <tab
        v-for="(service, i) in loadedServices"
        :key="service.service"
        :title="tabTitle(service.service, i)"
      >
        <div class="list-group">
          <btn
            type="plain"
            v-for="prov in service.providers"
            class="list-group-item"
            @click="chooseProviderAdd(service.service, prov.name)"
          >
            <plugin-info
              :detail="prov"
              :show-description="true"
              :show-extended="false"
              ><template #descriptionprefix> - </template>
            </plugin-info>
          </btn>
        </div>
      </tab>
    </tabs>
    <div class="list-group" v-else-if="loadedServices.length === 1">
      <btn
        type="plain"
        v-for="prov in loadedServices[0].providers"
        class="list-group-item"
        @click="chooseProviderAdd(loadedServices[0].service, prov.name)"
      >
        <plugin-info
          :detail="prov"
          :show-description="true"
          :show-extended="false"
          ><template #descriptionprefix> - </template>
        </plugin-info>
      </btn>
    </div>
    <template #footer>
      <btn @click="$emit('cancel')">{{ $t("Cancel") }}</btn>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { defineComponent } from "vue";
const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginModal",
  components: { pluginInfo },
  emits: ["cancel", "selected", "update:modelValue"],
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
  },
  methods: {
    tabTitle(service: string, i: number) {
      let name =
        this.tabNames && this.tabNames.length > i
          ? this.tabNames[i]
          : $t("plugin.type." + service + ".title.plural") || service;
      let count =
        this.loadedServices.find((s) => s.service === service)?.providers
          .length || 0;
      return name + " (" + count + ")";
    },
    chooseProviderAdd(service: string, provider: string) {
      this.$emit("selected", { service, provider });
      this.active = false;
    },
    findProvider(name: string) {
      return this.services.find((s) => s.name === name);
    },
  },
  data() {
    return {
      loadedServices: [],
      loading: false,
      modalShown: false,
    };
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
      return {
        service,
        providers: context.rootStore.plugins.getServicePlugins(service),
      };
    });
    this.loading = false;
    this.modalShown = this.modelValue;
  },
});
</script>

<style scoped lang="scss"></style>
