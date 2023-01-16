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

package common.security;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.User;
import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.event.rule.AfterActivationFiredEvent;
import org.drools.event.rule.DefaultAgendaEventListener;
import org.drools.runtime.StatelessKnowledgeSession;
import play.mvc.results.Forbidden;

/**
 * Metodi per il controllo della security.
 *
 * @author Marco Andreini
 */
@Singleton
@Slf4j
public class SecurityRules {

  private static final String CURRENT_OPERATOR_IDENTIFIER = "currentOperator";

  /**
   * Contiene in cache i risultati dei controlli sui permessi sulla richiesta corrente. <b>Nota:</b>
   * i controlli sono considerati per l'utente corrente.
   */
  private final Provider<Optional<User>> currentUser;
  private final Provider<String> currentAction;
  private final Provider<KnowledgeBase> knowledge;
  private final Provider<Map<PermissionCheckKey, Boolean>> checks;

  @Inject
  SecurityRules(Provider<Optional<User>> user,
      @Named("request.action") Provider<String> action,
      Provider<KnowledgeBase> knowledgeBase,
      @Named(SecurityModule.REQUESTS_CHECKS)
          Provider<Map<PermissionCheckKey, Boolean>> checks) {
    currentUser = user;
    currentAction = action;
    knowledge = knowledgeBase;
    this.checks = checks;
  }

  /**
   * Controlla se è possibile eseguire il metodo corrente sull'oggetto instance, e in caso negativo
   * riporta un accesso negato.
   */
  public void checkIfPermitted(Object instance) {
    if (!check(instance)) {
      // TODO: aggiungere messaggio più chiaro con permesso e operatore.
      throw new Forbidden("Access forbidden");
    }
  }

  /**
   * Corrisponde alla secure.check implicitamente eseguita su tutti i metodi.
   */
  public void checkIfPermitted() {
    if (!checkAction()) {
      // TODO: aggiungere messaggio più chiaro con permesso e operatore.
      throw new Forbidden("Access forbidden");
    }
  }

  /**
   * Controlla che l'azione corrente sia ammessa sull'instanza indicata.
   *
   * @return true se l'azione corrente è permessa sull'istanza fornita, false altrimenti.
   */
  public boolean check(Object instance) {
    return check(currentAction.get(), instance);
  }

  /**
   * Controlla se l'azione fornita è ammessa sull'instanza indicata.
   *
   * @return true se l'azione fornita è permessa sull'istanza fornita, false altrimenti.
   */
  public boolean check(String action, Object instance) {
    final PermissionCheck check = new PermissionCheck(instance, action);
    return doCheck(check);
  }

  /**
   * Controlla se l'azione corrente è permessa.
   *
   * @return true se l'azione corrente è permessa, false altrimenti.
   */
  public boolean checkAction() {
    return checkAction(currentAction.get());
  }

  /**
   * Controlla se l'azione fornita è permessa.
   *
   * @return true se l'azione fornita è permessa, false altrimenti.
   */
  public boolean checkAction(String action) {
    final PermissionCheck check = new PermissionCheck(null, action);
    return doCheck(check);
  }

  /**
   * Se il check fornito è già stato verificato, viene restituito il precedente già valorizzato,
   * altrimenti viene creato e valorizzato il nuovo check, salvandolo per le successive iterazioni.
   *
   * <p>Ovviamente il risultato della verifica è valida soltanto per la richiesta in corso
   * (ThreadLocal).</p>
   *
   * <p>Questa funzione ha l'effetto collaterale di impostare il check.granted.</p>
   *
   * @return true se il check fornito è permesso per lo user fornito.
   */
  private boolean doCheck(final PermissionCheck check) {
    if (checks.get().containsKey(check.getKey())) {
      return checks.get().get(check.getKey());
    }

    doCheck(check, currentUser.get().orNull());
    // salviamo il risultato per le iterazioni successive.
    // Nota bene: il risultato viene salvato per l'utente corrente.
    checks.get().put(check.getKey(), check.isGranted());
    return check.isGranted();
  }

  private void doCheck(final PermissionCheck check, final User user) {

    final StatelessKnowledgeSession session = knowledge.get().newStatelessKnowledgeSession();
    session.addEventListener(new AgendaLogger());

    log.debug("SecurityRules: currentUser = " + user);

    session.setGlobal(CURRENT_OPERATOR_IDENTIFIER, user);

    final List<Command<?>> commands = Lists.newArrayList();
    commands.add(CommandFactory.newInsert(check));
    commands.add(CommandFactory.newInsert(check.getTarget()));
    commands.add(CommandFactory.newInsert(user));
    commands.add(CommandFactory.newInsertElements(user.getRoles()));
    commands.add(CommandFactory.newInsertElements(user.getUsersRolesOffices()));
    session.execute(CommandFactory.newBatchExecution(commands));


    log.debug("{}", check);
  }

  private static class AgendaLogger extends DefaultAgendaEventListener {
    @Override
    public void afterActivationFired(AfterActivationFiredEvent event) {
      log.debug("RULE {} {}",
          event.getActivation().getRule().getName(), event.getActivation().getFactHandles());
    }
  }
}
