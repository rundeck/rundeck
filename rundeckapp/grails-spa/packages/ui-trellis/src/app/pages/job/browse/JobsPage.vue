<template>
    <div id="page_jobs" class="container-fluid">
        <div class="row">
            <div class="col-xs-12">
                <div class="card">
                    <div class="card-content">
                        <div class="runbox primary jobs" id="indexMain">
                            <job-list-page
                                :path="queryParams?.groupPath || ''"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="row">
            <div class="col-xs-12">
                <div class="card card-plain">
                    <div class="card-header">
                        <h3 class="card-title">
                            {{ $t("page.section.Activity.for.jobs") }}
                        </h3>
                    </div>
                </div>
                <div class="card" id="activity_section">
                    <div class="card-content">
                        <div class="_history_content">
                            <activity-list
                                :event-bus="EventBus"
                            ></activity-list>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {EventBus, getRundeckContext} from '@/library'
import { defineComponent } from "vue";
import JobListPage from "./JobListPage";
import ActivityList from "../../../components/activity/activityList.vue";

const eventBus=getRundeckContext().eventBus
export default defineComponent({
    name: "JobsPage",
    setup() {
        return {
            EventBus:eventBus,
        };
    },
    components: {
        JobListPage,
        ActivityList,
    },
    props: {
        queryParams: {
            type: Object,
            default: () => {
                return {};
            },
        },
    },
});
</script>

<style scoped lang="scss"></style>
