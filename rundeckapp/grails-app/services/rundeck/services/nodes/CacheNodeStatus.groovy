package rundeck.services.nodes

class CacheNodeStatus{

    String nodeName
    String executorReachable
    String executorTimeout
    Date lastChecktime
    Long checkDurationTime
    String statusDescription


    @Override
    public String toString() {
        return "CacheNodeStatus{" +
               "nodeName='" + nodeName + '\'' +
               ", executorReachable='" + executorReachable + '\'' +
               ", executorTimeout='" + executorTimeout + '\'' +
               ", lastChecktime=" + lastChecktime +
               ", checkDurationTime=" + checkDurationTime +
               ", statusDescription='" + statusDescription + '\'' +
               '}';
    }
}
