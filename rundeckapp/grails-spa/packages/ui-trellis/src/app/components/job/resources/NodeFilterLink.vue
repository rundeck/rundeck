<template>
  <a
    :href="href"
    :data-testid="dataTestId"
    class="xnodefilterlink"
    @click.prevent="handleClick"
  >
    <slot name="prefix"></slot>
    <slot>{{ getText() }}</slot>
    <slot name="suffix"></slot>
  </a>
</template>
<script lang="ts">
import { _genUrl } from "../../../../library/utilities/genUrl";
import { defineComponent } from "vue";
import { getRundeckContext, url } from "../../../../library";

export default defineComponent({
  name: "NodeFilterLink",
  props: {
    nodeFilterName: {
      type: String,
      required: false,
      default: "",
    },
    nodeFilter: {
      type: String,
      required: false,
      default: "",
    },
    exclude: {
      type: Boolean,
      required: false,
      default: false,
    },
    filterKey: {
      type: String,
      required: false,
      default: "",
    },
    filterVal: {
      type: String,
      required: false,
      default: "",
    },
    text: {
      type: String,
      required: false,
      default: "",
    },
  },
  emits: ["nodefilterclick"],
  data() {
    return {
      project: getRundeckContext().projectName,
    };
  },

  computed: {
    filterParam() {
      return this.exclude ? "filterExclude" : "filter";
    },
    filterParamValues() {
      const params = { [this.filterParam]: this.getFilter() };
      if (this.nodeFilterName) {
        params[this.exclude ? "filterNameExclude" : "filterName"] =
          this.nodeFilterName;
      }
      return params;
    },
    dataTestId() {
      return "nfl-" + this.sanitizeText(this.getText());
    },
    href() {
      return url(
        _genUrl(
          "/project/" + this.project + "/nodes",
          Object.assign({}, this.filterParamValues),
        ),
      ).href;
    },
  },
  methods: {
    handleClick() {
      this.$emit("nodefilterclick", this.filterParamValues);
    },
    sanitizeText(text: string | undefined): string {
      // replace non-alphanumeric characters with underscores to ensure the text is safe
      // to use as a property value
      return text ? text.replace(/[^a-zA-Z0-9-_]/g, "_") : "";
    },
    getText() {
      if (this.text) {
        return this.text;
      } else if (this.nodeFilterName) {
        return this.nodeFilterName;
      } else if (this.filterVal) {
        return this.filterVal;
      }
      return this.getFilter();
    },

    getFilter() {
      if (this.nodeFilter) {
        const nodeFilterCpy = this.nodeFilter.trim();
        const sepIdx = nodeFilterCpy.indexOf(": ");
        if (sepIdx < 0) {
          return nodeFilterCpy;
        }
        const valueStart = sepIdx + 2;
        const valueAndRest = nodeFilterCpy.slice(valueStart);
        // Already quoted — leave as-is to avoid double-quoting
        if (valueAndRest.startsWith('"')) {
          return nodeFilterCpy;
        }
        // Multi-attribute filter (e.g. "osFamily: unix name: localhost") —
        // detected by a space followed by an optional ! and an attribute name + colon.
        // Do NOT quote: the space is a separator between attributes, not part of a value.
        const hasMultipleAttrs = /\s+!?\w[\w.-]*:/.test(valueAndRest);
        if (!hasMultipleAttrs && valueAndRest.includes(" ")) {
          // Single attribute whose value contains spaces — quote it
          return nodeFilterCpy.slice(0, valueStart) + '"' + valueAndRest + '"';
        }
        return nodeFilterCpy;
      } else if (this.filterKey && this.filterVal) {
        return `${this.filterKey}: "${this.filterVal}"`;
      }
    },
  },
});
</script>
