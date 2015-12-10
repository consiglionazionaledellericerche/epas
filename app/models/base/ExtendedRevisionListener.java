package models.base;

import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.name.Named;

import injection.StaticInject;
import models.User;
import security.SecurityModule;

/**
 * @author marco
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
      } catch (ProvisionException e) {
        LOG.warn("unkown owner or user on {}: {}", revision, e);
      }
    } catch (NullPointerException ignored) {
      LOG.warn("NPE", ignored);
    }
  }
}
