package jobs;

import lombok.extern.slf4j.Slf4j;

import manager.BadgeManager;

import models.Badge;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;

import javax.inject.Inject;

/**
 * @author daniele
 * @since 30/06/16.
 */
@Slf4j
@OnApplicationStart(async = true)
public class BadgesNormalization extends Job {

  @Inject
  static BadgeManager badgeManager;

  public void doJob() {
    // Rimuove gli zeri iniziali se il codice badge è un intero.
    List<Badge> badges = Badge.findAll();
    for (Badge badge : badges) {
      badgeManager.normalizeBadgeCode(badge, true);
    }
    log.debug("Terminata normalizzazione Badges");
  }
}
