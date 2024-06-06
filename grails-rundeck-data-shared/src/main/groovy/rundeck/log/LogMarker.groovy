package rundeck.log

import org.slf4j.MDC

class LogMarker {
    static void markCustomerLog(Closure cls) {
        MDC.put("logtype","cust")
        cls()
        MDC.remove("logtype")
    }
}
