/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package common.security;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import common.injection.AutoRegister;
import controllers.Security;
import java.util.Map;
import models.User;
import org.drools.KnowledgeBase;
import play.Play;
import play.mvc.Http;

/**
 * Unione di injection con il play.
 *
 * @author Marco Andreini
 */
@AutoRegister
public class SecurityModule implements Module {

  /**
   * Interfaccia di security login.
   *
   * @author Cristian
   *
   */
  @FunctionalInterface
  public interface SecurityLogin {
    boolean isLegacy();
  }

  public static final String REMOTE_ADDRESS = "request.remoteAddress";
  public static final String REQUESTS_CHECKS = "requests.checks";
  public static final String NO_REQUEST_ADDRESS = "localhost";
  private static final String SECURITY_LEGACY = "security.legacy";
  private static final String SECURITY_LOGIN = "security.login";
  private static final String LEGACY = "legacy";

  @Provides
  @Named("request.action")
  public String currentAction() {
    return Http.Request.current().action;
  }

  /**
   * Fornisce la mappa con l'esito dei controlli correnti per l'injection.
   */
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
  @Named(SECURITY_LEGACY)
  public Boolean isSecurityLegacy() {
    return LEGACY.equalsIgnoreCase(Play.configuration.getProperty(SECURITY_LOGIN, LEGACY));
  }

  // questo per evitare il provider non digerito in automatico dal play-inject
  @Provides
  public SecurityLogin securityType(@Named(SECURITY_LEGACY) Boolean securityLegacy) {
    return () -> securityLegacy;
  }

  @Provides
  public KnowledgeBase knowledgeBase() {
    return SecureRulesPlugin.knowledgeBase;
  }

  /**
   * Fornisce l'operatore corrente per l'injection.
   */
  @Provides
  public Optional<User> currentOperator() {
    if (Http.Request.current() != null) {
      return Security.getUser();
    }
    return Optional.absent();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SecurityRules.class);
  }
}
