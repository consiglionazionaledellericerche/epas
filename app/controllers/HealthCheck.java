/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package controllers;

import controllers.Resecure.NoCheck;
import java.lang.management.ManagementFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

/**
 * Contiene metodi con informazioni sullo stato del sistema operativo
 * che ospita l'applicazione.
 *
 * @author Cristian Lucchesi
 *
 */
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