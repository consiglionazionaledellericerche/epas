package security;

import org.drools.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import controllers.Security;
import models.User;
import play.mvc.Http;

/**
 * Unione di injection con il play.
 *
 * @author marco
 */
public class SecurityModule implements Module {

  public final static String REMOTE_ADDRESS = "request.remoteAddress";
  private final static Logger log = LoggerFactory.getLogger(SecurityModule.class);

  @Provides
  @Named("request.action")
  public String currentAction() {
    return Http.Request.current().action;
  }

  @Provides
  @Named(REMOTE_ADDRESS)
  public String currentIpAddress() {
    return Http.Request.current().remoteAddress;
  }

  @Provides
  public KnowledgeBase knowledgeBase() {
    return SecureRulesPlugin.knowledgeBase;
  }

  @Provides
  public Optional<User> currentOperator() {
    Optional<User> user = Security.getUser();
    log.debug("SecurityModule: currentOperator = "
            + (user.isPresent() ? user.get() : "non presente"));
    return user;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SecurityRules.class);
  }
}
