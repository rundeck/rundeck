package com.dtolabs.rundeck.app.support

import spock.lang.Specification

/**
 * Created by greg on 1/6/16.
 */
class ExecutionQuerySpec extends Specification {
    def "parse relative date valid format"() {
        expect:
        null != ExecutionQuery.parseRelativeDate("1${unit}")

        where:
        unit | _
        "h"  | _
        "d"  | _
        "w"  | _
        "m"  | _
        "y"  | _
        "n"  | _
        "s"  | _
    }

    def "parse relative date invalid format"() {
        expect:
        null == ExecutionQuery.parseRelativeDate(fmt)

        where:
        fmt   | _
        "h"   | _
        "d1"  | _
        "1x"  | _
        " 1d" | _
        "1d " | _
        "1"   | _
        "-1d" | _
    }

    static Date mkdate(int y, int m, int d, int h, int n, int s) {
        Calendar instance = GregorianCalendar.getInstance()
        instance.setTimeInMillis(0)
        instance.set(y, m, d, h, n, s)
        instance.set(Calendar.MILLISECOND,0)
        instance.getTime()
    }

    def "parse relative date"() {
        given:
        Date date = mkdate(2015, 0, 2, 12, 25, 20)

        expect:
        0 == edate.compareTo(ExecutionQuery.parseRelativeDate(fmt, date))

        where:
        fmt   | edate
        "0d"  | mkdate(2015, 0, 2, 12, 25, 20)
        "1d"  | mkdate(2015, 0, 1, 12, 25, 20)
        "2d"  | mkdate(2014, 11, 31, 12, 25, 20)
        "10s" | mkdate(2015, 0, 2, 12, 25, 10)
        "10n" | mkdate(2015, 0, 2, 12, 15, 20)
        "1h"  | mkdate(2015, 0, 2, 11, 25, 20)
        "1w"  | mkdate(2014, 11, 26, 12, 25, 20)
        "1m"  | mkdate(2014, 11, 2, 12, 25, 20)
        "1y"  | mkdate(2014, 0, 2, 12, 25, 20)
    }
}
