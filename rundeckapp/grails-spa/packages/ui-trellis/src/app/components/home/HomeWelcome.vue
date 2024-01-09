<template>
  <div id="first-run-message" class="col-sm-12">
    <div class="card">
      <div class="card-header">
        <h2 class="card-title">
          {{ $t("app.firstRun.title", [appTitle, buildIdent]) }}
        </h2>
      </div>
      <div class="card-content">
        <img
          :src="`/assets/${logoImage}`"
          :alt="appTitle"
          style="width: 400px; padding-bottom: 10px"
          onload="SVGInject(this)"
        />
        <VMarkdownView
          :content="$t('app.firstRun.md', [appTitle, helpLinkUrl])"
          mode=""
        />
        <p class="h6 text-strong" style="margin-top: 1em">
          {{ $t("you.can.see.this.message.again.by.clicking.the") }}
          <a href="/menu/welcome">
            {{ $t("version.number") }}
          </a>
          {{ $t("in.the.page.footer") }}
        </p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { VMarkdownView } from "vue3-markdown";

export default defineComponent({
  name: "HomeWelcome",
  components: { VMarkdownView },
  props: {
    appTitle: {
      type: String,
      required: true
    },
    buildIdent: {
      type: String,
      required: true
    },
    logoImage: {
      type: String,
      required: true
    },
    helpLinkUrl: {
      type: String,
      required: true
    }
  }
});
</script>