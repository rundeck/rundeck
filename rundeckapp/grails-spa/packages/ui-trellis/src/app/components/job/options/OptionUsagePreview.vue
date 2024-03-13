<template>
  <!-- preview (text)  -->
  <section
    id="option_preview"
    class="section-separator-solo"
    v-if="option.name && option.type !== 'file' && !validationErrors['name']"
  >
    <div class="row">
      <label class="col-sm-2 control-label">{{ $t("usage") }}</label>
      <div
        class="col-sm-10 opt_sec_nexp_disabled"
        v-if="!option.secure || option.valueExposed"
      >
        <span class="text-strong">{{
          $t("the.option.values.will.be.available.to.scripts.in.these.forms")
        }}</span>
        <div>
          {{ $t("bash.prompt") }} <code>${{ bashVarPreview }}</code>
        </div>
        <div>
          {{ $t("commandline.arguments.prompt") }}
          <code>${option.{{ option.name }}}</code>
        </div>
        <div>
          {{ $t("commandline.arguments.prompt.unquoted") }}
          <code>${unquotedoption.{{ option.name }}}</code>
          {{ $t("commandline.arguments.prompt.unquoted.warning") }}
        </div>
        <div>
          {{ $t("script.content.prompt") }}
          <code>@option.{{ option.name }}@</code>
        </div>
      </div>
      <div class="col-sm-10 opt_sec_nexp_enabled" v-else>
        <span class="warn note">{{
          $t("form.option.usage.secureAuth.message")
        }}</span>
      </div>
    </div>
  </section>
  <!-- preview (file) -->
  <section
    id="file_option_preview"
    v-if="option.name && option.type === 'file' && !validationErrors['name']"
    class="section-separator-solo"
  >
    <div class="row">
      <label class="col-sm-2 control-label">{{ $t("usage") }}</label>
      <div class="col-sm-10">
        <span class="text-info">{{
          $t("form.option.usage.file.preview.description")
        }}</span>
        <div>
          {{ $t("bash.prompt") }} <code>${{ fileBashVarPreview }}</code>
        </div>
        <div>
          {{ $t("commandline.arguments.prompt") }}
          <code>${file.{{ option.name }}}</code>
        </div>
        <div>
          {{ $t("script.content.prompt") }}
          <code>@file.{{ option.name }}@</code>
        </div>

        <span class="text-info">{{
          $t("form.option.usage.file.fileName.preview.description")
        }}</span>
        <div>
          {{ $t("bash.prompt") }}
          <code>${{ fileFileNameBashVarPreview }}</code>
        </div>
        <div>
          {{ $t("commandline.arguments.prompt") }}
          <code>${file.{{ option.name }}.fileName}</code>
        </div>
        <div>
          {{ $t("script.content.prompt") }}
          <code>@file.{{ option.name }}.fileName@</code>
        </div>
        <span class="text-info">{{
          $t("form.option.usage.file.sha.preview.description")
        }}</span>
        <div>
          {{ $t("bash.prompt") }} <code>${{ fileShaBashVarPreview }}</code>
        </div>
        <div>
          {{ $t("commandline.arguments.prompt") }}
          <code>${file.{{ option.name }}.sha}</code>
        </div>
        <div>
          {{ $t("script.content.prompt") }}
          <code>@file.{{ option.name }}.sha@</code>
        </div>
      </div>
    </div>
  </section>
</template>
<script lang="ts">
import { JobOption } from "@/library/types/jobs/JobEdit";
import { defineComponent, PropType } from "vue";

const BashVarPrefix = "RD_";
export default defineComponent({
  props: {
    option: {
      type: Object as PropType<JobOption>,
      required: true,
    },
    validationErrors: {
      type: Object as PropType<Record<string, any>>,
      required: true,
    },
  },
  components: {},
  computed: {
    bashVarPreview() {
      return this.option.name ? this.tobashvar(this.option.name) : "";
    },
    fileBashVarPreview() {
      return this.option.name ? this.tofilebashvar(this.option.name) : "";
    },

    fileFileNameBashVarPreview() {
      return this.option.name
        ? this.tofilebashvar(this.option.name + ".fileName")
        : "";
    },
    fileShaBashVarPreview() {
      return this.option.name
        ? this.tofilebashvar(this.option.name + ".sha")
        : "";
    },
  },
  methods: {
    generateVariable: function (str: string) {
      return str
        .toUpperCase()
        .replace(/[^a-zA-Z0-9_]/g, "_")
        .replace(/[{}$]/, "");
    },
    tofilebashvar(str: string) {
      return BashVarPrefix + "FILE_" + this.generateVariable(str);
    },
    tobashvar(str: string) {
      return BashVarPrefix + "OPTION_" + this.generateVariable(str);
    },
  },
});
</script>
