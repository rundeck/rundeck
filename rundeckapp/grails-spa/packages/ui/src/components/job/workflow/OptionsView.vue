<template>
    <span :id="`opt${elemIdSuffix}`" class="optview">
        <span :class="['optdetail', editMode ? 'autohilite autoedit' : '']" :title="editMode ? 'Click to edit' : ''">
            <span v-if="option.optionType==='file'" class="glyphicon glyphicon-file" />
            <span :class="option.required ? 'required' : ''" :title="optionAltText()">{{ optionName }}</span>
            <span class="">
                <span :title="{showTitle:truncatedText}" :class="{wasTruncated: 'truncatedtext'}">
                    {{ truncateText(secureInput, {max: 20, front: false, showTitle: true}) }}
                    {{ showMultivalueIcon }}
                </span>
                <span v-show="showLockIcon" class="glyphicon glyphicon-lock" />
            </span>
            <span class="desc">{{ optionDesc }}</span>
        </span>
        <span v-if="valuesExist">
            <VTooltip style="display:inline;">
                <span class="valuesSet">
                    <span class="valueslist">
                        {{ valuesDesc }}
                    </span>
                </span>
                <template #popper>
                    <div class="popout detailpopup" style="width:200px;" >
                        <div class="info note">{{ $t('label.allowedVals') }}</div>
                        <div v-for="val in optionVals" :key="`${val}_${index}`">
                            {{ index != 0 ? ', ' : '' }}
                            <span class="valueItem">{{ val }}</span>
                        </div>
                    </div>
                </template>
            </VTooltip>
        </span>
        <span v-else-if="option.realValuesUrl">
            <span class="valuesSet">
                <span
                    class="valuesUrl"
                    :title="$t('message.valuesLoadedRemoteUrl', [option.realValuesUrl])"
                >
                    {{ $t('label.url') }}
                </span>
            </span>
        </span>
        <span v-if="option.enforced">
            <span class="enforceSet">
                <span
                class="enforced"
                :title="$t('message.inputMustBeAllowedValues')"
            >
                {{ $t('label.strict') }}
            </span>
            </span>
        </span>
        <span v-else-if="option.regex">
            <VTooltip>
                <span class="enforceSet">
                    <span class="regex">
                        {{ option.regex }}
                    </span>
                </span>
                <template #popper>
                    <div class="popout detailpopup" style="width: 200px">
                        <div class="info note">{{ $t('message.valuesMustMatchRegex') }}</div>
                        <code>{{ option.regex }}</code>
                    </div>
                </template>
            </VTooltip>
        </span>
        <span v-else>
            <span class="enforceSet">
                <span class="any" :title="$t('message.noRestrictionsInputValue')">{{ $t('label.none') }}</span>
            </span>
        </span>
    </span>
</template>

<script lang="ts">
  import Vue from 'vue';
  import VTooltipPlugin from 'v-tooltip';
  Vue.use(VTooltipPlugin);

  const w = window as any;

  export default Vue.extend({
    name: 'OptionsView',
    components: {
      VTooltipPlugin
    },
    props: {
        option: Object,
        editMode: Boolean,
        elemIdSuffix: String,
    },
    computed: {
      optionName: function(): string {
        return JSON.stringify(this.option.name);
      },
      optionVals: function(): string[] {
        return this.option.optionValues;
      },
      valuesDesc: function(): string {
        const optSize = this.optionVals ? this.optionVals.length : 0;
        const label = `Value${optSize == 1 ? '' : 's'}`;
        return `${optSize} ${label}`;
      },
      valuesExist: function(): boolean {
          return (this.option.values || this.option.valuesList);
      },
      secureInput: function(): string {
        if (this.option.secureInput && this.option.defaultValue) {
            return '****';
        } else {
            return this.option.defaultValue;
        }
      },
      showTitle: function(): boolean {
          return this.truncSettings.showTitle;
      },
      wasTruncated: function(): boolean {
          return this.truncSettings.wasTruncated;
      },
      truncatedText: function(): string {
          return this.truncSettings.text;
      },
      showMultivalueIcon: function(): string {
          if (this.option.multivalued) {
              return '(+)';
          }
          return "";
      },
      showLockIcon: function(): boolean {
      return this.option.secureInput && this.option.defaultStoragePath;
      },
      optionDesc: function(): string {
          let tmp = document.createElement("DIV");
          tmp.innerHTML = this.option.description;
          return tmp.textContent || tmp.innerText || "";
      }
    },
    methods: {
      optionAltText: function() {
          let title = this.option.description;
          if (this.option.required) {
              title += " (Required)";
          };
          return title;
      },
      truncateText: function(text: string, options = {max: 30, showTitle: false, front: false}) {
        if (!text) {
          return "";
        }
        const max = options.max;
        const front = options.front;
        this.truncSettings.originalText = text;
        this.truncSettings.showTitle = options.showTitle;
        let newText = text.trim();
        if (newText.length > max) {
          this.truncSettings.wasTruncated = true;
        }
        if (front) {
            const beginIndex = (text.length - max);
            newText = "..." + text.slice(beginIndex);
        } else {
            newText = text.slice(0, max);
            newText += "...";
        }
        this.truncSettings.text = newText;
        return newText;
      }
    },
    data() {
      return {
        deleteConfirm: false,
        truncSettings: {
          originalText: "",
          text: "",
          wasTruncated: false,
          showTitle: false
      }
        }
    }
  })
</script>
