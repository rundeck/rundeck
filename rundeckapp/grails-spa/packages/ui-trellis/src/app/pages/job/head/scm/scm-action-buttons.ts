import { defineComponent, markRaw, provide, reactive } from 'vue'
import { getRundeckContext } from '../../../../../library'

import moment from 'moment'
import JobScmActions from '@/app/pages/job/browse/tree/JobScmActions.vue'
import { JobBrowseItem } from "../../../../../library/types/jobs/JobBrowse";
import { JobPageStoreInjectionKey } from "../../../../../library/stores/JobPageStore";
import { observer } from "../../../../utilities/uiSocketObserver";

const rootStore = getRundeckContext().rootStore;
const jobPageStore = reactive(rootStore.jobPageStore);
moment.locale(getRundeckContext().locale||'en_US')
rootStore.ui.addItems([
    {
        section: "job-head",
        location: "job-action-button",
        visible: true,
        widget: markRaw(
            defineComponent({
                name: "JobHeadScmActions",
                components: { JobScmActions },
                props: ["itemData"],
                setup(props){
                    let job : JobBrowseItem = reactive({ job: true, groupPath: "", id: props.itemData.jobUuid })
                    provide(JobPageStoreInjectionKey, jobPageStore);

                    jobPageStore.load()
                    jobPageStore.getJobBrowser().loadJobMeta(job.id).then(jobMeta => job.meta = jobMeta)

                    return { jobPageStore, job }
                },
                template: `<job-scm-actions :job="job"></job-scm-actions>`
            })
        ),
    },
]);

window.addEventListener('DOMContentLoaded', () => {
    let jobActionsButtons = document.querySelectorAll('[id^="action-menu-jobrow_"]');
    for (let elem of jobActionsButtons) {
      if (elem) {
        observer.observe(elem, { subtree: true, childList: true });
      }
    }
})