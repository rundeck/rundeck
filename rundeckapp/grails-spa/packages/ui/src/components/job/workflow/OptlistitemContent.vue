<template>
  <div :id="`optvis_${optionName}`" >
    <div class="optitem optctrlholder">
      <span v-if=edit class="optctrl opteditcontrols controls ">
        <span v-if="w.canMoveUp || w.canMoveDown" class="dragHandle btn btn-xs" :title="$t('drag.to.reorder')">
        <i class="glyphicon glyphicon-resize-vertical"></i>
      </span>
    <span v-else class="btn btn-xs btn-default disabled" ><i class="glyphicon glyphicon-resize-vertical"></i></span>
  <div class="btn-group">
    <span v-if=canMoveUp class="btn btn-xs btn-default" @click="reorder(optionName, -1)"
                          :title="$t('move.up')">
      <i class="glyphicon glyphicon-arrow-up"></i>
    </span>
    <span v-else class="btn btn-xs btn-default disabled" >
      <i class="glyphicon glyphicon-arrow-up"></i>
    </span>

    <span v-if=canMoveDown class="btn btn-xs btn-default" @click="reorder(optionName, 1)"
          :title="$t('move.down')">
      <i class="glyphicon glyphicon-arrow-down"></i>
    </span>
    <span v-else class="btn btn-xs btn-default disabled" >
      <i class="glyphicon glyphicon-arrow-down"></i>
    </span>
    </div>
  </span>

  <span class="opt item" :id="optionName" >
    <opt-view :option="option" :editMode="editMode" />
  </span>

  <div id="optdel_${enc(attr:ukey)}" class="panel panel-danger collapse">
    <div class="panel-heading">
      <g:message code="delete.this.option" />
    </div>

    <div class="panel-body">
      <g:message code="really.delete.option.0" args="${[option.name]}"/>
    </div>

    <g:jsonToken id="reqtoken_del_${ukey}" url="${request.forwardURI}"/>

    <div class="panel-footer">
                <span class="btn btn-default btn-xs"
                      onclick="jQuery('#optdel_${enc(js:ukey)}').collapse('toggle');"><g:message code="cancel"/></span>
      <span class="btn btn-danger btn-xs"
            @click="w._doRemoveOption(optionName, jQuery(this).closest('li.optEntry'),'reqtoken_del_${enc(js:ukey)}');"><g:message
        code="delete"/></span>
    </div>
  </div>

  <div v-if="editMode">
    <g:jsonToken id="reqtoken_duplicate_${ukey}" url="${request.requestURI}"/>

    <span class="optctrl opteditcontrols controls " :id="`optctrls_${optionName}`" style="position:absolute; right:0;">
            <span class="btn btn-xs btn-info" @click="w._optedit(optionName,jQuery(this).closest('li.optEntry'));"
                  title="${message(code:"edit.this.option")}">
                <i class="glyphicon glyphicon-edit"></i>
                <g:message code="edit" />
            </span>
    <span class="btn btn-xs btn-info" @click="w._optcopy(optionName,'reqtoken_duplicate_${ukey}');"
          title="${message(code:"duplicate.this.option")}">
    <i class="glyphicon glyphicon-duplicate"></i>
    <g:message code="duplicate" />
    </span>
    <span class="btn btn-xs btn-danger "
          data-toggle="collapse"
          data-target="#optdel_${enc(attr:ukey)}"
          title="${message(code:"delete.this.option")}">
    <i class="glyphicon glyphicon-remove"></i>
    </span>
    </span>
  </div>
  <g:if test="${edit}">

    <g:javascript>
      fireWhenReady('opt_${enc(js:option.name)}',function(){
      var options = jQuery('#opt_${enc(js:option.name)}').find( '.autoedit' )
      options.each(function (indx, elem) {
      elem.addEventListener('click', function(evt){
      _optedit('${enc(js:option.name)}',jQuery(this).closest('li.optEntry'));
      }, false);
      });
      });
    </g:javascript>
  </g:if>
</div>
</div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import VueI18n from 'vue-i18n'
  import i18n from './i18n'
  Vue.use(VueI18n)

  const w = window as any
  const _i18n = i18n as any
  const lang = _window._rundeck.language|| 'en'
  const locale = _window._rundeck.locale|| 'en_US'

  const messages = {
    [locale]: {
      ...(_i18n[lang] || _i18n.en),
      ...(_window.Messages[lang])
    }
  }

  const i18nInstance = new VueI18n({
    silentTranslationWarn: true,
    locale: locale,
    messages
  })


export default {
  name: 'OptlistitemContent',
  components: {i18n: i18nInstance},
  props: {
    option: Object,
    editMode: Boolean,
    optIndex: Number,
    optCount: Number
  },
  mounted() {
    generateUkey: () {

    }
  },
  computed: {
    canMoveUp: function() {
      return this.optIndex != 0;
    },
    canMoveDown: function() {
      return this.optIndex < this.optCount - 1;
    },
    optionName: function() {
      return JSON.stringify(this.option.name);
    }
  },
  methods: {
    reorder(value: string, pos: number) {
      w._doReorderOption(value, {pos: pos});
    },
    // this method imitates rkey from UtilityTagLib
    generateUkey() {
      
    },
    jsonToken(id, url) {
      const uri = url || request.forwardURI;
      const token = generateToken(uri);
      embedJson.call({id:id, data: {TOKEN: token, URI: uri}}, body)
    }
  },
  data() {
    return {
      editMode: this.editMode,
      ukey: null
    }
  }
}
</script>
