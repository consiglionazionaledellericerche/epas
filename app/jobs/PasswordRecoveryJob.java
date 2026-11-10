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

package jobs;

import common.injection.StaticInject;
import dao.PersonDao;
import javax.inject.Inject;
import manager.EmailManager;
import manager.UserManager;
import models.Person;
import play.jobs.Job;

/**
 * Genera il token di recupero password e invia la mail.
 *
 * <p>È un job perché la risposta alla richiesta non deve dipendere dall'indirizzo: né i tempi né
 * gli errori devono rivelare se un'email è registrata. Gli errori sono scritti nel log.</p>
 */
@StaticInject
public class PasswordRecoveryJob extends Job<Void> {

  @Inject
  static PersonDao personDao;
  @Inject
  static UserManager userManager;
  @Inject
  static EmailManager emailManager;

  private final String email;

  public PasswordRecoveryJob(String email) {
    this.email = email;
  }

  @Override
  public void doJob() {
    final Person person = personDao.byEmail(email).orNull();
    if (person != null) {
      userManager.generateRecoveryToken(person);
      emailManager.recoveryPasswordMail(person);
    }
  }
}
