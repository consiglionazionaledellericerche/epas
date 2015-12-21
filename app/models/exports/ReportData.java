package models.exports;

import java.util.List;

import lombok.Data;

/**
 * Dati prelevati via json dalle segnalazioni degli utenti.
 *
 * @author marco
 * @author dario
 */
@Data
public class ReportData {

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
}
