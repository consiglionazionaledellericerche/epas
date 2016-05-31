package security;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import controllers.Security;

import models.User;

import org.drools.KnowledgeBase;

import play.mvc.Http;

import java.util.Map;

/**
 * Unione di injection con il play.
 *
 * @author marco
 */
public class SecurityModule implements Module {

  public static final String REMOTE_ADDRESS = "request.remoteAddress";
  public static final String REQUESTS_CHECKS = "requests.checks";
  public static final String NO_REQUEST_ADDRESS = "localhost";

  @Provides
  @Named("request.action")
  public String currentAction() {
    return Http.Request.current().action;
  }

  @Named(REQUESTS_CHECKS)
  @Provides
  public Map<PermissionCheckKey, Boolean> currentChecks() {
    if (!Http.Request.current().args.containsKey(REQUESTS_CHECKS)) {
      Http.Request.current().args
          .put(REQUESTS_CHECKS, Maps.<PermissionCheckKey, Boolean>newHashMap());
    }
    return (Map<PermissionCheckKey, Boolean>) Http.Request.current().args.get(REQUESTS_CHECKS);
  }

  @Provides
  @Named(REMOTE_ADDRESS)
  public String currentIpAddress() {
    return Http.Request.current() != null ? Http.Request.current().remoteAddress
        : NO_REQUEST_ADDRESS;
  }

  @Provides
  public KnowledgeBase knowledgeBase() {
    return SecureRulesPlugin.knowledgeBase;
  }

  @Provides
  public Optional<User> currentOperator() {
    return Security.getUser();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SecurityRules.class);
  }
}
