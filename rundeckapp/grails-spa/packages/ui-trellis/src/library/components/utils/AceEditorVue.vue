<template>
  <div :id="identifier" ref="root" :style="styleCss"></div>
</template>
<script lang="ts">
import { defineComponent, PropType } from "vue";

import ace, { Ace } from "ace-builds";
import { ContextVariable } from "../../stores/contextVariables";

export default defineComponent({
  props: {
    identifier: {
      type: String,
      default: "",
    },
    modelValue: {
      type: String,
      required: true,
    },
    height: {
      type: String,
      default: "",
    },
    width: {
      type: String,
      default: "",
    },
    lang: {
      type: String,
      default: "",
    },
    softWrap: {
      type: Boolean,
      default: true,
    },
    theme: {
      type: String,
      default: "",
    },
    darkTheme: {
      type: String,
      default: "",
    },
    options: {
      type: Object as PropType<Ace.EditorOptions>,
      default: () => {},
    },
    contextVariableSuggestions: {
      type: Array<ContextVariable>,
      default: () => [],
    },
  },
  emits: ["init", "update:modelValue"],
  data(): {
    editor: Ace.Editor | null;
    contentBackup: string;
    observer: MutationObserver | null;
    jsonSpaces: number;
  } {
    return {
      editor: null,
      contentBackup: "",
      observer: null,
      jsonSpaces: 2,
    };
  },
  computed: {
    styleCss() {
      const style = { height: "100%", width: "100%" };
      if (this.height) style.height = this.px(this.height);
      if (this.width) style.width = this.px(this.width);
      return style;
    },
  },
  watch: {
    modelValue: function (val: string): void {
      if (this.contentBackup !== val) {
        this.editor!.getSession().setValue(this.resolveValue(val));
        this.contentBackup = this.resolveValue(val);
      }
    },
    theme: function (newTheme): void {
      this.editor!.setTheme(this.resolveTheme(newTheme));
    },
    lang: function (newLang): void {
      this.editor!.getSession().setMode(this.resolveLang(newLang));
    },
    softWrap: function (newSoftWrap): void {
      this.editor!.getSession().setUseWrapMode(newSoftWrap);
    },
    options: function (newOption): void {
      this.editor!.setOptions(newOption);
    },
    height: function (): void {
      this.$nextTick(function () {
        this.editor!.resize();
      });
    },
    width: function (): void {
      this.$nextTick(function () {
        this.editor!.resize();
      });
    },
  },
  mounted: function () {
    const lang = this.lang || "text";
    const theme = this.getTheme();

    require("ace-builds/src-noconflict/ext-emmet");

    const editor = (this.editor = ace.edit(this.$el));

    this.$emit("init", editor);
    editor.getSession().setUseWorker(false);
    editor.getSession().setMode(this.resolveLang(lang));
    editor.setTheme(this.resolveTheme(theme));

    const shouldEnableAutoCompletion =
      this.contextVariableSuggestions && this.contextVariableSuggestions.length > 0;
    editor.setOptions({
      ...(this.options || {}),
      enableLiveAutocompletion: shouldEnableAutoCompletion,
    });

    if (this.modelValue) editor.setValue(this.resolveValue(this.modelValue), 1);

    this.contentBackup = this.modelValue;

    this.addContextVariableSuggestions();
    this.observeDarkMode();
    this.attachChangeEventToEditor();
  },
  beforeUnmount: function () {
    this.editor!.destroy();
    this.editor!.container.remove();
    this.observer?.disconnect();
  },
  methods: {
    /**
     * Observe the dark mode and update the theme for the editor
     */
    observeDarkMode() {
      const query = matchMedia("(prefers-color-scheme: dark)");

      const changeHandler = () => {
        this.editor!.setTheme(this.resolveTheme(this.getTheme()));
      };

      // Support for Safari <14
      if (typeof query.addEventListener == "function")
        query.addEventListener("change", changeHandler);
      else query.addListener(changeHandler);

      this.observer = new MutationObserver(changeHandler);
      this.observer.observe(document.documentElement, { attributes: true });
    },

    /**
     * Get the theme to use based on whether it is dark mode or not
     */
    getTheme() {
      let theme = document.documentElement.dataset.colorTheme || "light";

      if (theme == "system")
        theme = matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";

      return theme == "dark" ? this.darkTheme || "tomorrow_night_eighties" : this.theme || "chrome";
    },
    px(value: string): string {
      if (/^\d*$/.test(value)) return `${value}px`;

      return value;
    },
    resolveTheme(theme: string): string {
      return `ace/theme/${theme}`;
    },
    resolveLang(lang: string): string {
      return typeof lang === "string" ? `ace/mode/${lang}` : lang;
    },
    /**
     * Convert value to supported format
     *
     * @param val
     */
    resolveValue(val: string) {
      const LANG_JSON: string = "json";
      try {
        if (this.lang == LANG_JSON) {
          return JSON.stringify(JSON.parse(val), null, this.jsonSpaces);
        }
        return val;
      } catch (e) {
        return val;
      }
    },
    /**
     * Attach change event to ace editor
     *
     */
    attachChangeEventToEditor(): void {
      this.editor!.on("change", () => {
        const content = this.editor!.getValue();
        this.$emit("update:modelValue", content);
        this.contentBackup = content;
      });
    },

    addContextVariableSuggestions(): void {
      const staticWordCompleter: Ace.Completer = {
        ...this.buildCompletionProvider(),
        ...this.buildDocTooltipProvider(),
        identifierRegexps: [/[@%a-zA-Z_0-9.\$\-\u00A2-\uFFFF]/],
      } as Ace.Completer;

      this.editor!.completers = [staticWordCompleter];
    },

    buildCompletionProvider() {
      return {
        getCompletions: (
          _editor: Ace.Editor,
          _session: Ace.EditSession,
          _pos: Ace.Point,
          _prefix: string,
          callback: Ace.CompleterCallback,
        ) => {
          const suggestions = this.contextVariableSuggestions.map(
            ({ name, type, description, title }) => ({
              name,
              title,
              desc: description,
              value: name,
              meta: type[0].toUpperCase() + type.slice(1),
            }),
          );

          callback(null, suggestions);
        },
      };
    },

    buildDocTooltipProvider() {
      const lang = ace.require("ace/lib/lang");
      return {
        getDocTooltip: (item: Ace.Completion & { title: string; desc?: string }) => {
          const title = lang.escapeHTML(item.title);
          const desc = item.desc ? `<br><hr>${lang.escapeHTML(item.desc)}` : "";
          item.docHTML = `<b>${title}${desc}</b>`;
        },
      };
    },
  },
});
</script>
