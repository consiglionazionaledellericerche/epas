/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import models.User;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifiche sulla gestione degli hash delle password: storico, tempi di risposta e login
 * contemporanei durante l'aggiornamento dell'hash.
 */
public class PasswordHashingSecurityTest extends UnitTest {

  private static final String PASSWORD = "unaPasswordDiTest";
  private static final int SAMPLES = 10;

  /**
   * Gli hash non salati (MD5 e SHA-512) non finiscono nello storico degli utenti.
   */
  @Test
  public void unsaltedHashesAreNotAudited() {
    String previous = play.Play.configuration.getProperty(User.KEEP_MD5_PASSWORD);
    play.Play.configuration.setProperty(User.KEEP_MD5_PASSWORD, "true");
    try {
      User user = new User();
      user.setUsername("audit." + UUID.randomUUID());
      user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
      user.updatePassword(PASSWORD);
      user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
      user.save();
      commit();

      @SuppressWarnings("unchecked")
      List<Object[]> revisions = AuditReaderFactory.get(JPA.em()).createQuery()
          .forRevisionsOfEntity(User.class, false, true)
          .add(org.hibernate.envers.query.AuditEntity.id().eq(user.id))
          .getResultList();
      assertFalse(revisions.isEmpty());
      for (Object[] revision : revisions) {
        User audited = (User) revision[0];
        assertNull(audited.getPassword());
        assertNull(audited.getPasswordSha512());
        assertNull(audited.getPasswordHash());
      }
    } finally {
      restore(previous);
    }
  }

  /**
   * Un login fallito impiega un tempo confrontabile per utenti esistenti e inesistenti, così
   * dai tempi di risposta non si capisce se uno username esiste.
   */
  @Test
  public void failedLoginTimeDoesNotRevealUsername() {
    User user = new User();
    user.setUsername("timing." + UUID.randomUUID());
    user.updatePassword(PASSWORD);
    user.save();

    // riscaldamento
    Security.authenticate(user.getUsername(), "sbagliata");
    Security.authenticate("inesistente." + UUID.randomUUID(), "sbagliata");

    long existing = averageNanos(user.getUsername());
    long missing = averageNanos("inesistente." + UUID.randomUUID());
    assertTrue(String.format("esistente %d ns, inesistente %d ns", existing, missing),
        missing * 2 > existing);
  }

  /**
   * Più login contemporanei dello stesso utente con password da aggiornare (es. client di
   * timbratura in Basic Auth) riescono tutti.
   */
  @Test
  public void concurrentLoginsDuringRehashSucceed() throws Exception {
    final String username = "concurrent." + UUID.randomUUID();
    User user = new User();
    user.setUsername(username);
    user.setPassword(User.cryptPasswordMd5(PASSWORD));
    user.setPasswordSha512(User.cryptPasswordSha512(PASSWORD));
    user.save();
    commit();

    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      List<Callable<Boolean>> logins = Lists.newArrayList();
      for (int i = 0; i < 4; i++) {
        logins.add(() -> {
          try {
            return JPA.withTransaction("default", false,
                () -> Security.authenticate(username, PASSWORD));
          } catch (Throwable e) {
            throw new Exception(e);
          }
        });
      }
      for (Future<Boolean> result : executor.invokeAll(logins)) {
        assertTrue(result.get());
      }
    } finally {
      executor.shutdown();
    }
    JPA.em().clear();
    assertNotNull(((User) User.find("byUsername", username).first()).getPasswordHash());
  }

  private long averageNanos(String username) {
    long total = 0;
    for (int i = 0; i < SAMPLES; i++) {
      long start = System.nanoTime();
      assertFalse(Security.authenticate(username, "sbagliata"));
      total += System.nanoTime() - start;
    }
    return total / SAMPLES;
  }

  private void restore(String previous) {
    if (previous == null) {
      play.Play.configuration.remove(User.KEEP_MD5_PASSWORD);
    } else {
      play.Play.configuration.setProperty(User.KEEP_MD5_PASSWORD, previous);
    }
  }

  private void commit() {
    JPA.em().getTransaction().commit();
    JPA.em().getTransaction().begin();
  }
}
