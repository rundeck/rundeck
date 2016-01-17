import rundeck.services.StoredJobChangeEvent

events = {
    jobChanged filter:StoredJobChangeEvent, fork:true
}