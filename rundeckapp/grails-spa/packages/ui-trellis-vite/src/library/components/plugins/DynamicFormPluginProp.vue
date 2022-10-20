
<template>
    <div id="fieldcustomeditor" class="col-sm-12">
        <hr/>

        <div v-if="customFields!=null">
            <div v-for="(field, index) in customFields" :key="index" :class="['form-group']" >
                <label class="col-sm-2 control-label input-sm">{{ field.label||field.key }}</label>
                <div class="col-sm-9">
                    <input  v-model="field.value" type="text" :class="['form-control','input-sm','context_var_autocomplete']" size="100" @change="changeField(field)" >
                </div>
                <div class="col-sm-1">
                <span class="btn btn-xs btn-default " @click="removeField(field)" :title="$t('message.delete')">
                <i class="glyphicon glyphicon-remove"></i></span>

                </div>
                <div class="col-sm-10 col-sm-offset-2 help-block" v-if="field.desc">
                    <div class="help-block">{{field.desc}}</div>
                </div>
            </div>
        </div>

        <btn type="primary" @click="openNewField()">{{ $t('message.addField') }}</btn>

        <modal v-model="modalAddField" title="Add Field" ref="modal" id="modal-demo" ok-text="Save" :backdrop="true"
               :dismiss-btn="true"
               :keyboard="true"
               cancel-text="Close" append-to-body>
            <div class="row" style="padding-left: 30px !important;">
                <alert type="warning" v-if="duplicate"><b>Warning!</b> {{ $t('message.duplicated') }}.</alert>


                <div class="col-md-10">
                    <div class="form"  v-if="useOptions">
                        <div :class="['form-data']">
                            <label class="col-md-4">{{ $t('message.select') }}</label>
                            <div class="col-md-8">

                              <multiselect v-model="selectedField" :options="customOptions"  track-by="label" label="label"
                                           :placeholder="$t('message.select' )">

                              </multiselect>


                            </div>
                        </div>

                        <div :class="['form-data']" >
                            <label class="col-md-4">{{ $t('message.description') }}</label>
                            <div class="col-md-8">
                                <input type="text" v-model="newFieldDescription" :class="['form-control']">
                                <div class="help-block">{{ $t('message.empty') }}</div>
                            </div>
                        </div>
                    </div>

                    <div class="form" v-if="!useOptions">
                        <div :class="['form-group']">
                            <label class="col-md-4">{{ $t('message.fieldLabel') }}</label>
                            <div class="col-md-8">
                                <input type="text" v-model="newLabelField" :class="['form-control']">
                            </div>
                        </div>
                        <div :class="['form-group']" >
                            <label class="col-md-4">{{ $t('message.fieldKey') }}</label>
                            <div class="col-md-8">
                                <input type="text" v-model="newField" :class="['form-control']">
                            </div>
                        </div>

                        <div :class="['form-group']" >
                            <label class="col-md-4">{{ $t('message.description') }}</label>
                            <div class="col-md-8">
                                <input type="text" v-model="newFieldDescription" :class="['form-control']">
                                <div class="help-block">{{ $t('message.empty') }}</div>

                            </div>
                        </div>
                    </div>
                </div>
            </div>


            <div slot="footer">
                <button type="button" class="btn btn-default reset_page_confirm" @click="modalAddField=false">
                    {{ $t('message.cancel') }}
                </button>

                <button type="button" class="btn btn-cta reset_page_confirm" @click="addField()">
                    {{ $t('message.add') }}
                </button>
            </div>
        </modal>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import VueI18n from 'vue-i18n'
    import i18n from './i18n'
    Vue.use(VueI18n)
    import Multiselect from 'vue-multiselect'

    const _window = window as any
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

    import {Component, Prop, Watch} from 'vue-property-decorator'

    Vue.component('Multiselect', Multiselect)

    @Component({
      i18n: i18nInstance
    })
    export default class DynamicFormPluginProp extends Vue{
        @Prop({required:true}) readonly fields!: string
        @Prop({required:false}) readonly options!: string
        @Prop({required:true}) readonly element!: string
        @Prop({required:true}) readonly hasOptions!: string
        @Prop({required:true}) readonly name!: string


        customFields:  any[] = []
        customOptions:  any[] = []

        useOptions = false
        modalAddField = false
        duplicate = false
        newField: string = ''
        newLabelField: string = ''
        newFieldDescription: string = ''
        selectedField = {value:"", label:""} as any

        openNewField() {
            this.modalAddField =true;
        }

        addField() {
            let field = {} as any;
            this.duplicate = false;

            if(this.useOptions){

                if(this.selectedField !== null){

                    const newField = this.selectedField;

                    let description = this.newFieldDescription;
                    if(description == ''){
                      description = 'Field key ' + newField.value
                    }else{
                      description = description + ' (Field key: ' + newField.value + ')';
                    }

                    field = {key: newField.value, label: newField.label, desc: description } ;

                }
            }else {
                let description = this.newFieldDescription;
                if(description == ''){
                    description = 'Field key ' + this.newField
                }else{
                    description = description + ' (Field key: ' + this.newField + ')';
                }

                field = {key: this.newField, label: this.newLabelField, value: '', desc: description}
            }

            let exists = false;
            this.customFields.forEach((row: any) => {
                if (field.key === row.key) {
                    exists = true;
                }
            });


            if(!exists){
                this.customFields.push(field);
                this.newField = '';
                this.newLabelField = '';
                this.newFieldDescription = '';
                this.modalAddField = false;

                this.refreshPlugin();
            }else{
                this.duplicate = true;
            }

        }

        removeField(row: any) {
            const fields = [] as any;

            this.customFields.forEach((field: any) => {
                if (field.key !== row.key) {
                    fields.push(field);
                }
            });

            this.customFields = fields;
            this.refreshPlugin();

        }

        changeField(field: any){
            this.refreshPlugin();
        }

        refreshPlugin(){
            const fieldsJson = JSON.stringify(this.customFields);
            const customFields = document.getElementById(this.element) as HTMLInputElement;
            customFields.value = fieldsJson;

          this.$emit('input', fieldsJson);

        }

        mounted(){
            if(this.hasOptions === 'true' ){
                this.useOptions = true
            }
            if(this.fields!=null && this.fields !== '' ){
                const customFieldsObject =  JSON.parse(this.fields);
                if(customFieldsObject != null){
                    const fields = Object.keys(customFieldsObject).map((key: any) => {
                        const value = customFieldsObject[key];
                        if (value.desc == null){
                            value.desc = 'Field key: ' + value.key
                        }
                        return value;
                    });
                    this.customFields = fields;
                }
            }

            if(this.hasOptions && this.options!=null && this.options !== '' ) {
                const optionsObject =  JSON.parse(this.options);
                const options = Object.keys(optionsObject).map((key: any) => {
                    const data = optionsObject[key];
                    return {value: key, label: data};
                });
                this.customOptions = options;
            }
        }

        beforeDestroy () {
            this.customFields = null as any;
        }


    }
</script>
<style scoped src="vue-multiselect/dist/vue-multiselect.min.css"></style>

<style scoped>

.form-data{
  padding-bottom: 30px;
  margin-bottom: 20px;
}
</style>
