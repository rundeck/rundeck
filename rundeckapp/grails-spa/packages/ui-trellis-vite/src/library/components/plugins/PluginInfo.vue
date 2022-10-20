<template>
  <span>
    <slot></slot>
    <span v-if="showIcon">
      <img class="plugin-icon" :src="iconUrl" v-if="iconUrl">
      <i :class="'glyphicon glyphicon-'+glyphicon" v-else-if="glyphicon"></i>
      <i :class="'fas fa-'+faicon" v-else-if="faicon"></i>
      <i :class="'fab fa-'+fabicon" v-else-if="fabicon"></i>
      <i class="rdicon icon-small plugin" v-else></i>
    </span>
    <span :class="titleCss" v-if="showTitle" style="margin-left: 5px;">{{title}}</span>
    <span :class="descriptionCss" v-if="showDescription" style="margin-left: 5px;">{{shortDescription}}</span>
    <details class="more-info details-reset" :class="extendedCss" v-if="showDescription && showExtended && extraDescription">
        <summary>
            More...
            <span class="more-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-right"/></span>
            <span class="less-indicator-verbiage more-info-icon"><i class="glyphicon glyphicon-chevron-down"/></span>
        </summary>

    {{extraDescription}}

    </details>

    <slot name="suffix"></slot>
  </span>
</template>
<script lang="ts">
import Vue from "vue";

export default Vue.extend({
    name: 'PluginInfo',
    components: {
    },
    props: {
        'showIcon': {
            'type': Boolean,
            'default': true,
            'required': false
        },
        'showTitle': {
            'type': Boolean,
            'default': true,
            'required': false
        },
        'titleCss':{
            type:String,
            default:'text-strong',
            required:false
        },
        'showDescription': {
            'type': Boolean,
            'default': true,
            'required': false
        },
        'descriptionCss':{
            type:String,
            default:'',
            required:false
        },
        'showExtended': {
            'type': Boolean,
            'default': true,
            'required': false
        },
        'extendedCss':{
            type:String,
            default:'text-muted',
            required:false
        },
        'detail': {
            'type': Object,
            'required': true
        }
    },
    data: function () {
        return {
            toggleExtended: false
        }
    },
    computed: {
        description() :string {
            return this.detail.description|| this.detail.desc;
        },
        title() :string{
            return this.detail.title;
        },
        providerMeta(): any{
          return this.detail && this.detail.providerMetadata|| {}
        },
        iconUrl():string {
            return this.detail.iconUrl;
        },
        glyphicon() :string{
            return this.providerMeta.glyphicon;
        },
        faicon() :string{
            return this.providerMeta.faicon;
        },
        fabicon() :string{
            return this.providerMeta.fabicon;
        },
        shortDescription() :string{
          const desc = this.description
            if (desc && desc.indexOf("\n") > 0) {
                return desc.substring(0, desc.indexOf("\n"));
            }
            return desc;
        },
        extraDescription() :string|null{
          const desc = this.description
            if (desc && desc.indexOf("\n") > 0) {
                return desc.substring(desc.indexOf("\n") + 1);
            }
            return null;
        }
    },

})
</script>

<style scoped lang="scss">
.plugin-icon {
    width: 16px;
    height: 16px;
    border-radius: 2px;
}

</style>