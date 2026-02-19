<template>
  <div class="col-sm-12">
    <div class="form-group">
      <label v-if="!ea" data-testid="plugin-search-label" for="stepFilter" class="col-sm-2 control-label">
        {{ $t("step.plugins.filter.prompt") }}
      </label>
      <div v-if="!ea" class="col-sm-10">
        <div class="input-group stepfilters">
          <input
            id="stepFilter"
            v-model="filterValue"
            data-testid="plugin-search-input"
            type="search"
            name="nodeFilter"
            class="schedJobStepFilter form-control allowenter"
            :placeholder="$t('enter.a.step.filter.override')"
            @keydown.enter.prevent="filterStepDescriptions"
          />

          <div class="input-group-btn">
            <popover
              append-to="body"
              trigger="hover-focus"
              placement="bottom"
              custom-class="popover-wide"
            >
              <btn type="button">
                <i class="glyphicon glyphicon-question-sign"></i>
              </btn>
              <template #popover>
                <div class="help-block">
                  <strong>{{ $t("workflow.search.help.string1") }}</strong>
                  <p>
                    <code>{{ $t("workflow.search.help.string2") }}</code>
                  </p>
                  <p>
                    {{ $t("workflow.search.help.string3") }}
                  </p>

                  <strong>{{ $t("workflow.search.help.string4") }}</strong>

                  <ul>
                    <li>
                      {{ $t("workflow.search.help.string5") }}
                      <code>{{ $t("workflow.search.help.string6") }}</code>
                    </li>
                    <li>
                      {{ $t("workflow.search.help.string7") }}
                      <code>{{ $t("workflow.search.help.string8") }}</code>
                    </li>
                    <li>
                      {{ $t("workflow.search.help.string9") }}
                      <code>{{ $t("workflow.search.help.string10") }}</code>
                    </li>
                  </ul>

                  <p>{{ $t("workflow.search.help.string11") }}</p>
                  <ul>
                    <li>
                      {{ $t("workflow.search.help.string12") }}
                      <code>{{ $t("workflow.search.help.string13") }}</code>
                    </li>
                    <li>
                      {{ $t("workflow.search.help.string14") }}
                      <code>{{ $t("workflow.search.help.string15") }}</code>
                    </li>
                    <li>
                      {{ $t("workflow.search.help.string16") }}
                      <code>{{ $t("workflow.search.help.string17") }}</code>
                    </li>
                  </ul>
                </div>
              </template>
            </popover>

            <btn data-testid="plugin-search-button" @click="filterStepDescriptions">
              {{ $t("search") }}
            </btn>
          </div>
        </div>
      </div>
      <div v-else>
        <PtInput
            v-model="filterValue"
            type="search"
            name="nodeFilter"
            :placeholder="$t('enter.a.step.filter.override')"
            left-icon="pi pi-search"
            input-id="stepFilter"
            @keydown.enter.prevent="filterStepDescriptions"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { PtInput } from "@/library/components/primeVue";

export default defineComponent({
  name: "PluginSearch",
  components: {
    PtInput,
  },
  props: {
    ea: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["search", "searching"],
  data() {
    return {
      filterValue: "",
      debounceTimer: null as number | null,
    };
  },
  watch: {
    filterValue(newValue: string) {
      // Only apply live search with debounce in EA mode
      if (!this.ea) {
        return;
      }

      // Emit searching state immediately
      this.$emit("searching", true);

      // Clear existing timer
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer);
      }

      // Set new timer for debounced search
      this.debounceTimer = window.setTimeout(() => {
        this.filterStepDescriptions();
        this.$emit("searching", false);
      }, 300);
    },
  },
  methods: {
    filterStepDescriptions() {
      this.$emit("search", this.filterValue);
    },
  },
  beforeUnmount() {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  },
});
</script>

<style scoped>
.input-group-btn > span > .btn {
  border-radius: 0;
}
</style>
