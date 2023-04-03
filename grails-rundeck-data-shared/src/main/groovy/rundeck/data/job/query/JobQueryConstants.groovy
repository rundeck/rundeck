package rundeck.data.job.query

class JobQueryConstants {
    /**
     * text filters
     */
    public final static TEXT_FILTERS = [
            job:'jobName',
            desc:'description',
    ]
    /**
     * equality filters
     */
    public final static EQ_FILTERS=[
            loglevel:'loglevel',
            proj:'project',
            jobExact:'jobName',
            serverNodeUUID:'serverNodeUUID'
    ]
    /**
     * Boolean filters
     */
    public final static  BOOL_FILTERS=[
            'executionEnabled':'executionEnabled',
            'scheduleEnabled':'scheduleEnabled',
    ]

    /**
     * Scheduled filter
     */
    public final static IS_SCHEDULED_FILTER = [
            'scheduled':'scheduled'
    ]

    /**
     * all filters
     */
    public final static  ALL_FILTERS = [ :]
    public final static  X_FILTERS = [ :]
    static{
        ALL_FILTERS.putAll(TEXT_FILTERS)
        ALL_FILTERS.putAll(EQ_FILTERS)
        ALL_FILTERS.putAll(BOOL_FILTERS)
        ALL_FILTERS.putAll(IS_SCHEDULED_FILTER)
        X_FILTERS.putAll(ALL_FILTERS)
        X_FILTERS.put('group','groupPath')
    }
}
