package controllers;

import java.lang.management.ManagementFactory;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Controller;
import play.mvc.Http;

@Slf4j
public class HealthCheck extends Controller {

  public final static int LOAD_WARNING_THRESHOLD = 4;
  
  /**
   * Controlla il carico del sistema operativo ospitante
   * l'applicazione.
   */
  public static void status() {
    val operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
    val load = operatingSystemBean.getSystemLoadAverage();
    val threads = ManagementFactory.getThreadMXBean();
    
    if (load > LOAD_WARNING_THRESHOLD) {
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