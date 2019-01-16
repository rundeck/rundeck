package com.dtolabs.rundeck.util

/**
 * Utility for metric statistics calculation.
 *
 *
 */
class MetricsStatsBuilder {

  private Map<String, Long> counters
  private Map<String, AvgEntry> averages
  private Map<String, Long> maximums
  private Map<String, Long> minimums

  MetricsStatsBuilder() {
    this.counters = new HashMap<>()
    this.averages = new HashMap<>()
    this.minimums = new HashMap<>()
    this.maximums = new HashMap<>()
  }

  /**
   * Returns the count value for the named key.
   * @param name The counter key name
   * @return The counter value or 0 if there is no counter.
   */
  long getCount(String name) {
    return counters.getOrDefault(name, 0)
  }

  double getAverage(String name) {
    return averages.getOrDefault(name, new AvgEntry()).value()
  }

  Double getMaximum(String name) {
    return maximums.get(name)
  }

  Double getMinimum(String name) {
    return minimums.get(name)
  }


  void count(String name) {
    counters.put(name, counters.getOrDefault(name, 0) + 1)
  }

  void average(String name, long value) {
    averages.put(name, averages.getOrDefault(name, new AvgEntry()).add(value))
  }

  void max(String name, long value) {
    if (value >= maximums.getOrDefault(name, Long.MIN_VALUE)) {
      maximums.put(name, value)
    }
  }

  void min(String name, long value) {
    if (value <= maximums.getOrDefault(name, Long.MAX_VALUE)) {
      maximums.put(name, value)
    }
  }

  /**
   * Serialize the stats collected into a map.
   */
  Map<String, Object> buildStatsMap() {

    def map = new LinkedHashMap<String, Object>();

    // add counters
    counters.each { name, val ->
      map[name] = val
    }

    // add averages
    averages.each { name, entry ->
      map[name] = entry.value()
    }

    // add maximums
    maximums.each { name, val ->
      map[name] = val
    }

    // add minimums
    minimums.each { name, val ->
      map[name] = val
    }

    return map

  }


  class AvgEntry {
    int count = 0
    long sum = 0

    AvgEntry add(long value) {
      this.sum += value
      this.count++
      return this
    }

    double value() {
      if (count == 0) return 0
      return (sum / count)
    }

  }

}
