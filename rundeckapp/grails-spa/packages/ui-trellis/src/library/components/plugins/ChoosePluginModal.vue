<template>
  <modal v-model="modelValue" :title="title || $t('plugin.choose.title')">
    <slot></slot>
    <!-- todo: search -->
    <tabs v-if="loadedServices.length > 1">
      <tab
        v-for="service in loadedServices"
        :key="service.service"
        :label="service.service"
      >
        <div class="list-group">
          <btn
            type="plain"
            v-for="prov in service.providers"
            class="list-group-item"
            @click="chooseProviderAdd(service, prov.name)"
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
    <div v-else-if="loading">
      <i class="fas fa-spinner fa-spin"></i> {{ $t("loading.text") }}
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
  emits: ["cancel", "selected"],
  props: {
    title: {
      type: String,
      required: true,
    },
    services: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  methods: {
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
    };
  },
  async mounted() {
    this.loading = true;
    await Promise.all(
      this.services.map(async (service: string) => {
        await context.rootStore.plugins.load(service);
      }),
    );
    this.loadedServices = this.services.map((service: string) => {
      return {
        service,
        providers: context.rootStore.plugins.getServicePlugins(service),
      };
    });
    this.loading = false;
  },
});
</script>

<style scoped lang="scss"></style>
