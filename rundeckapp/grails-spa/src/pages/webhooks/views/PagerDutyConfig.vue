<template>
    <div>
        <div class="form-group">
            <label class="control-label">Batch Key</label>
            <input class="form-control" type="text" v-model="config.batchKey" placeholder="Batch Key">
            <span class="help-block">Optional JsonPath to key containing items to treat as individual events</span>
        </div>
        <div>
            <div class="h2" style="margin-top:0;">
                <span>Rules</span>
                <div class="btn btn-md btn-success" @click="() => addNewRule('front')">
                    Add
                </div>
            </div>
            
        </div>

        <div class="panel panel-default" v-for="(rule, index) in config.rules" :key="index">
            <div class="panel-heading">
                <div class="form-inline">
                    <!-- <div class="form-group"> -->
                        <div class="input-group" style="margin-bottom:0;">
                            <input class="form-control" v-model="rule.name" placeholder="Rule Name">
                            <div class="input-group-addon btn btn-md btn-danger"
                                @click="handleDeleteRuleSet(rule)">
                                DELETE
                            </div>
                        </div>
                    <!-- </div> -->
                    <div class="btn btn-primary pull-right" type="button" data-toggle="collapse" :data-target="'#' + index + 'rulePanel'" aria-expanded="false" aria-controls="collapseExample" style="display:inline;">
                        <i class="glyphicon glyphicon-edit"></i>
                    </div>
                </div>
                
            </div>
            <div class="panel-body collapse" :id="index + 'rulePanel'">
                <div class="form-group">
                    <label class="control-label">JobID</label>
                    <input class="form-control" type="text" v-model="rule.jobId" placeholder="Job ID">
                </div>

                <!-- <div class="form-group">
                    <label class="control-label">Job Arg String</label>
                    <input class="form-control" type="text" v-model="rule.jobArgString" placeholder="Job Args">
                </div> -->

                <div class="form-group">
                    <label class="control-label">Node Filter</label>
                    <input class="form-control" type="text" v-model="rule.nodeFilter" placeholder="Node Filter">
                </div>

                <div class="h4">
                    <span>Job Options</span>

                    <div class="btn btn-md btn-success" @click="addNewJobOption(rule)">
                        Add
                    </div>
                </div>
                
                <div style="padding-left:10px;">
                    <div v-for="(option, index) in rule.jobOptions" :key="`options-${index}`">
                        <div style="width:100%;margin-bottom:5px;display:flex;align-items:center;">
                            
                            <input class="form-control" type="text" v-model="option.name" placeholder="Name">

                            <input class="form-control" type="text" v-model="option.value" placeholder="Value/JsonPath" style="margin-left:5px;">

                            <div class="form-group" style="margin:0 5px 0;">
                                <div class="btn btn-squircle btn-md btn-danger"
                                    @click="handleDeleteOption(rule,option)">
                                        Delete
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="h4">
                    <span>Conditions</span>

                    <div class="btn btn-md btn-success" @click="addNewCondition(rule)">
                        Add
                    </div>
                </div>

                <div style="padding-left:10px;">
                    <div class="form-group">
                        <label>Policy</label>
                        <select v-model="rule.policy">
                            <option>all</option>
                            <option>any</option>
                        </select>
                    </div>
                    <div v-for="(condition,index) in rule.conditions" :key="index">
                        <div style="width:100%;margin-bottom:5px;display:flex;align-items:center;">
                            
                                <input class="form-control" type="text" v-model="condition.path" placeholder="Path">

                                <select class="form-control input-sm" v-model="condition.condition" style="width:100px;margin:0px 5px 0px;">
                                    <option>contains</option>
                                    <option>exists</option>
                                    <option>matches</option>
                                </select>

                                <input class="form-control" type="text" v-model="condition.value" placeholder="Value">
                                <div class="form-group" style="margin:0 5px 0;">
                                    <div class="btn btn-squircle btn-md btn-danger"
                                        @click="handleDeleteCondition(rule,condition)">
                                            Delete
                                    </div>
                                </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div v-if="config.rules.length > 0" class="btn btn-md btn-success" @click="() => addNewRule('end')">
            Add Rule
        </div>
    </div>
</template>

<script>
export default {
    name: "PagerDutyConfig",
    props: {
        curHook: Object,
    },
    watch: {
        // Ensure the config gets setup if the parent changes the hook
        curHook: function(newVal, oldVal) {
            this.init()
        }
    },
    data() {
        return {
            webhooks: [],
            webhookPlugins: [],
            config: null,
        }
    },
    created: function() {
        this.init()
    },

    methods: {
        /**
         * Construct the config template if not set
         */
        init() {
            if (this.curHook.config == undefined || Object.keys(this.curHook.config).length == 0) {
                this.curHook.config = {
                    rules: [{
                        name: '',
                        description: '',
                        jobId: '',
                        policy: '',
                        jobArgString: '',
                        jobOptions: [],
                        nodeFilter: '',
                        conditions: []
                    }]
                }
            }
            this.config = this.curHook.config
        },
        addNewRule(side) {
            if (! this.config.rules instanceof Array)
                this.config.rules = []

            let rule ={
                jobId: '',
                policy: 'any',
                conditions: []
            }

            if (side == 'front')
                this.config.rules.unshift(rule)
            else if (side == 'end')
                this.config.rules.push(rule)
            else
                console.error('Side must be front|end; not sure why I am checking this')

        },
        addNewCondition(rule) {
            rule.conditions.push({})
        },
        handleDeleteRuleSet(prop) {
            let index = this.config.rules.indexOf(prop)
            if (index >  -1)
                this.config.rules.splice(index, 1)
        },
        handleDeleteCondition(prop, condition) {
            let index = prop.conditions.indexOf(condition)
            if (index >  -1)
                prop.conditions.splice(index, 1)
        },

        handleDeleteOption(rule, option) {
            let index = rule.jobOptions.indexOf(option)
            if (index > -1)
                rule.jobOptions.splice(index, 1)
        },
        addNewJobOption(rule) {
            if (rule.jobOptions == undefined)
                this.$set(rule, 'jobOptions', [{}])
            else
                rule.jobOptions.push({})
        }

    }
}
</script>


<style lang="scss" scoped>
  .btn-squircle {
    border-radius: 5px;
  }
</style>
