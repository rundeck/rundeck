package org.rundeck.app.components.jobs.stats;

public interface JobStatsProvider {
    JobStats calculateJobStats(String uuid);

    interface JobStats {

        double getSuccessRate();

        long getExecCount();

        long getAverageDuration();

        static JobStats with(
                double successRate,
                long execCount,
                long averageDuration
        )
        {
            return new JobStats() {
                @Override
                public double getSuccessRate() {
                    return successRate;
                }

                @Override
                public long getExecCount() {
                    return execCount;
                }

                @Override
                public long getAverageDuration() {
                    return averageDuration;
                }
            };
        }
    }
}
