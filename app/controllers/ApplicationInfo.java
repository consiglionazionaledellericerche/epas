package controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import controllers.Resecure.NoCheck;
import dao.GeneralSettingDao;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import lombok.val;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Util;
import play.mvc.With;

/**
 * Gestisce le informazioni sull'applicazione e le informazioni di privacy.
 *
 */
@With(Resecure.class)
public class ApplicationInfo extends Controller {

  @Inject
  private static GeneralSettingDao generalSettingDao;

  @NoCheck
  public static void privacyPolicy() {
    if (generalSettingDao.generalSetting().isCookiePolicyEnabled()) {
      val content = generalSettingDao.generalSetting().getCookiePolicyContent();
      render(content);
    }
    notFound();
  }

  @Util
  public static boolean isCookiePolicyEnabled() {
    return generalSettingDao.generalSetting().isCookiePolicyEnabled();
  }

  @Util
  public static boolean isRegulationsEnabled() {
    return generalSettingDao.generalSetting().isRegulationsEnabled();
  }

  /**
   * Preleva l'informazione della versione corrente dal file VERSION utilizzando
   * la cache se il dato è presente.
   */
  @Util
  public static String getVersion() {
    String version = Cache.get("VERSION", String.class);
    if (version == null) {
      try {
        version = Files.asCharSource(new File("VERSION"), Charsets.UTF_8).read();
      } catch (IOException e) {
        version = "unknown";
      }
      Cache.add("VERSION", version);
    }
    return version;
  }
  
}
