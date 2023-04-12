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

package models.base;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.name.Named;
import common.injection.StaticInject;
import common.security.SecurityModule;
import javax.inject.Inject;
import models.User;
import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Revision listener che aggiunge le informazioni su owner e ipaddress che hanno modificato
 * la revisione.
 *
 * @author Marco Andreini
 */
@StaticInject
public class ExtendedRevisionListener implements RevisionListener {

  private static final Logger LOG = LoggerFactory.getLogger(ExtendedRevisionListener.class);

  @Inject
  static Provider<Optional<User>> user;
  @Inject
  @Named(SecurityModule.REMOTE_ADDRESS)
  static Provider<String> ipaddress;

  @Override
  public void newRevision(Object revisionEntity) {
    try {
      final Revision revision = (Revision) revisionEntity;
      try {
        revision.ipaddress = ipaddress.get();
        revision.owner = user.get().orNull();
      } catch (ProvisionException ex) {
        LOG.warn("unkown owner or user on {}: {}", revision, ex);
      }
    } catch (NullPointerException ignored) {
      LOG.warn("NPE", ignored);
    }
  }
}
