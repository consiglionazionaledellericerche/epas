/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package models.exports;

import com.google.common.collect.Maps;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

/**
 * Dati prelevati via json dalle segnalazioni degli utenti.
 *
 * @author Marco Andreini
 * @author Dario Tagliaferri
 * @author Cristian Lucchesi
 */
@Data
public class ReportData implements Serializable {

  /**
   * DTO che contiene i dati relativi al browser con cui viene
   * effettuata la segnalazione.
   */
  @ToString
  @Data
  public static class BrowserData {
    private String appCodeName;
    private String appName;
    private String appVersion;
    private boolean cookieEnabled;
    private boolean onLine;
    private String platform;
    private String userAgent;
    private List<String> plugins;
  }

  private BrowserData browser;
  private String html;
  private byte[] img;
  private String note;
  private String url;
  private String category;
  private Map<String, String> session = Maps.newHashMap();

  @Override
  public String toString() {
    return String.format("ReportData[BrowserData=%s, url=%s, category=%s, html.size()=%s,"
        + " img.size()=%s, note=%s]", browser, url, category, html != null ? html.length() : 0,
            img != null ? img.length : 0, note);
  }
}