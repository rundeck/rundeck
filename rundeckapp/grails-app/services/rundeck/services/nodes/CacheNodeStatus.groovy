package rundeck.services.nodes

class CacheNodeStatus{

    String nodeName
    String executorReachable
    Date lastChecktime
    Long checkDurationTime
    String statusDescription


    @Override
    public String toString() {
        return "CacheNodeStatus{" +
               "nodeName='" + nodeName + '\'' +
               ", executorReachable='" + executorReachable + '\'' +
               ", lastChecktime=" + lastChecktime +
               ", checkDurationTime=" + checkDurationTime +
               ", statusDescription='" + statusDescription + '\'' +
               '}';
    }
}
