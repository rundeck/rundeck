import EntryTypes from "@/app/components/notification-center/EntryTypes";

export const NotificationCenterEntries = [
    {
        id: 0,
        entry_type: EntryTypes.TASK,
        title: "Asynchronous Project Import",
        started_at: "Fri Jan 12 2024 21:00:51",
        status: "In progress...",
        completed_proportion: 10,
        progress_proportion: 4
    },
    {
        id: 1,
        entry_type: EntryTypes.TASK,
        title: "Project Import",
        started_at: "Fri Jan 13 2024 23:00:51",
        status: "Almost there...",
        completed_proportion: 5,
        progress_proportion: 4
    },
    {
        id: 2,
        entry_type: EntryTypes.TASK,
        title: "Scm Job Replication",
        started_at: "Fri Jan 06 2024 13:00:52",
        status: "Completed",
        completed_proportion: 5,
        progress_proportion: 5
    },
    {
        id: 3,
        entry_type: EntryTypes.TASK,
        title: "Scm Job Replication",
        started_at: "Fri Jan 06 2024 13:00:52",
        status: "Completed",
        completed_proportion: 5,
        progress_proportion: 0
    }
]