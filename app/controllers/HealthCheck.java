package controllers;

import controllers.Resecure.NoCheck;
import java.lang.management.ManagementFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

@Slf4j
@With({Resecure.class})
public class HealthCheck extends Controller {

  public static final String DEFAULT_LOAD_WARNING_THRESHOLD = "4";
  
  /**
   * Controlla il carico del sistema operativo ospitante
   * l'applicazione.
   */
  @NoCheck
  public static void status() {
    val operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
    val load = operatingSystemBean.getSystemLoadAverage();
    val threads = ManagementFactory.getThreadMXBean();
    int loadWarningThreshold = 
        Integer.parseInt(
            Play.configuration.getProperty(
                "play.load.warning.threashold", DEFAULT_LOAD_WARNING_THRESHOLD));

    if (load > loadWarningThreshold) {
      for (val threadInfo : threads.dumpAllThreads(true, true)) {
        log.info("thread (id={}) {} -> status = {}, "
            + "cpuTime = {}, userTime = {}, "
            + "stackTrace = {}.\n{}", 
            threadInfo.getThreadId(),
            threadInfo.getThreadName(),
            threadInfo.getThreadState(),
            threads.getThreadCpuTime(threadInfo.getThreadId()),
            threads.getThreadUserTime(threadInfo.getThreadId()),
            threadInfo.getStackTrace(),
            threadInfo.toString());      
      }
      response.status = Http.StatusCode.OVERLOADED;
      response.print("ko. Load: " + load);
    } else {
      renderText("ok. Load: " + load);  
    }
    log.debug("Load: {}", load);
  }
}