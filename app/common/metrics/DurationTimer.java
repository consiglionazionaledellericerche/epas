package common.metrics;

import lombok.val;

public class DurationTimer {

  private long start;
  
  public static DurationTimer start() {
    val timer = new DurationTimer();
    timer.start = System.currentTimeMillis();
    return timer;
  }

  public long stop() {
    return System.currentTimeMillis() - start;
  }
}
