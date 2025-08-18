<template>
  <modal
    v-model="showModal"
    :title="title || $t('plugin.edit.title')"
    size="lg"
  >
    <div v-if="provider">
      <p>
        <plugin-info
          :detail="provider"
          :show-description="true"
          :show-extended="false"
          description-css="ml-5"
        ></plugin-info>
      </p>
      <plugin-config
        v-model="editModel"
        :mode="pluginConfigMode"
        :plugin-config="provider"
        :show-title="false"
        :show-description="false"
        :context-autocomplete="true"
        :validation="validation"
        scope="Instance"
        default-scope="Instance"
        group-css=""
        description-css="ml-5"
        data-testid="plugin-info"
        :service-name="serviceName"
      ></plugin-config>
      <slot name="extra"></slot>
    </div>
    <div v-else-if="loading">
      <p>
        <i class="fas fa-spinner fa-spin"></i>
        {{ $t("loading.text") }}
      </p>
    </div>
    <template #footer>
      <btn @click="$emit('cancel')" data-testid="cancel-button">
        {{ $t("Cancel") }}
      </btn>
      <btn type="success" @click="saveChanges" data-testid="save-button">
        {{ $t("Save") }}
      </btn>
    </template>
  </modal>
</template>
<script lang="ts">
import pluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { PluginConfig } from "@/library/interfaces/PluginConfig";
import { getServiceProviderDescription } from "@/library/modules/pluginService";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";

export default defineComponent({
  name: "EditPluginModal",
  components: { pluginInfo, pluginConfig },
  props: {
    title: {
      type: String,
      required: true,
    },
    modelValue: {
      type: Object,
      required: true,
      default: () => ({}) as PluginConfig,
    },
    serviceName: {
      type: String,
      required: true,
    },
    modalActive: {
      type: Boolean,
      required: false,
      default: true,
    },
    validation: {
      type: Object,
      required: false,
      default: () => ({}),
    },
  },
  emits: ["cancel", "save", "update:modelValue", "update:modalActive"],
  data() {
    return {
      showModalVal: this.modalActive,
      editModel: {} as PluginConfig,
      provider: null,
      loading: false,
      pluginConfigMode: "edit",
    };
  },
  computed: {
    showModal: {
      get() {
        return this.showModalVal && (this.loading || !!this.provider);
      },
      set(val: boolean) {
        this.showModalVal = val;
        this.$emit("update:modalActive", val);
      },
    },
  },
  watch: {
    modalActive(val) {
      this.showModalVal = val;
    },
    async modelValue(val) {
      this.editModel = cloneDeep(val);
      await this.loadProvider();
    },
  },
  async mounted() {
    this.editModel = cloneDeep(this.modelValue);
    if (
      this.modelValue.config &&
      Object.keys(this.modelValue.config).length === 0
    ) {
      this.pluginConfigMode = "create";
    }
    await this.loadProvider();
  },
  methods: {
    async saveChanges() {
      this.$emit("update:modelValue", this.editModel);
      this.$emit("save");
    },
    async loadProvider() {
      if (this.editModel.type) {
        try {
          this.loading = true;
          this.provider = await getServiceProviderDescription(
            this.serviceName,
            this.editModel.type,
          );
        } catch (e) {
          console.log(e);
        } finally {
          this.loading = false;
        }
      } else {
        this.loading = false;
        this.provider = null;
      }
    },
  },
});
</script>
