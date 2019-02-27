<template>
  <span>
    <span v-if="showIcon">
      <img :src="iconUrl" v-if="iconUrl" width="16px" height="16px">
      <i :class="'glyphicon glyphicon-'+glyphicon" v-else-if="glyphicon"></i>
      <i :class="'fas fa-'+faicon" v-else-if="faicon"></i>
      <i class="rdicon icon-small plugin" v-else></i>
    </span>
    <span class="text-info" v-if="showTitle">{{title}}</span>
    <span class="text-muted" v-if="showDescription">{{shortDescription}}</span>
    <span class="text-muted" v-if="showExtended && extraDescription">
      <span @click="toggleExtended=!toggleExtended">More...</span>
      <span v-if="toggleExtended">
        {{extraDescription}}
      </span>
    </span>
  </span>
</template>
<script lang="ts">
import Vue from "vue";

export default Vue.extend({
    name: 'PluginInfo',
    components: {
        // motd,
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
        'showDescription': {
            'type': Boolean,
            'default': true,
            'required': false
        },
        'showExtended': {
            'type': Boolean,
            'default': true,
            'required': false
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
            return this.detail.desc;
        },
        title() :string{
            return this.detail.title;
        },

        iconUrl():string {
            return this.detail.iconUrl;
        },
        glyphicon() :string{
            return this.detail.glyphicon;
        },
        faicon() :string{
            return this.detail.faicon;
        },
        shortDescription() :string{
            if (this.description && this.description.indexOf("\n") > 0) {
                return this.description.substring(0, this.description.indexOf("\n"));
            }
            return this.description;
        },
        extraDescription() :string|null{
            if (this.description && this.description.indexOf("\n") > 0) {
                return this.description.substring(this.description.indexOf("\n") + 1);
            }
            return null;
        }
    },

})
</script>
