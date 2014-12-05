package security;

import java.util.List;

import models.User;

import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.event.rule.AfterActivationFiredEvent;
import org.drools.event.rule.DefaultAgendaEventListener;
import org.drools.runtime.StatelessKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.results.Forbidden;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author marco
 *
 */
@Singleton
public class SecurityRules {
	
	private final static Logger LOG = LoggerFactory.getLogger(SecurityRules.class);
	
	private final Provider<Optional<User>> currentUser;
	private final Provider<String> currentAction;
	private final Provider<KnowledgeBase> knowledge;
	
	private final static String CURRENT_OPERATOR_IDENTIFIER = "currentOperator";
	
	@Inject
	SecurityRules(Provider<Optional<User>> user, 
			@Named("request.action") Provider<String> action,
			Provider<KnowledgeBase> knowledgeBase) {
		currentUser = user;
		currentAction = action;
		knowledge = knowledgeBase;
	}
	
	/**
	 * Controlla se è possibile eseguire il metodo corrente sull'oggetto
	 * instance, e in caso negativo riporta un accesso negato.
	 * 
	 * @param instance
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
	 * @param instance
	 * @return true se l'azione corrente è permessa sull'istanza fornita, false altrimenti.
	 */
	public boolean check(Object instance) {
		return check(currentAction.get(), instance);
	}
	
	/**
	 * @param action
	 * @param instance
	 * @return true se l'azione fornita è permessa sull'istanza fornita, false altrimenti.
	 */
	public boolean check(String action, Object instance) {
		final PermissionCheck check = new PermissionCheck(instance, action);
		return doCheck(check, currentUser.get().orNull());
	}
	
	/**
	 * @param action
	 * @param role
	 * @return true se l'azione corrente è permessa, false altrimenti.
	 */
	public boolean checkAction() {
		return checkAction(currentAction.get());
	}

	/**
	 * @param action
	 * @return true se l'azione fornita è permessa, false altrimenti.
	 */
	public boolean checkAction(String action) {
		final PermissionCheck check = new PermissionCheck(null, action);
		return doCheck(check, currentUser.get().orNull());
	}
	
	private boolean doCheck(final PermissionCheck check, final User user) {
        
        final StatelessKnowledgeSession session = knowledge.get().newStatelessKnowledgeSession();
        session.addEventListener(new AgendaLogger());
        
        LOG.debug("SecurityRules: currentUser = " + user);
        
        session.setGlobal(CURRENT_OPERATOR_IDENTIFIER, user);
        
        final List<Command<?>> commands = Lists.newArrayList();
        commands.add(CommandFactory.newInsert(check));
        commands.add(CommandFactory.newInsert(check.getTarget()));
        commands.add(CommandFactory.newInsert(user));
        commands.add(CommandFactory.newInsertElements(user.usersRolesOffices));
        session.execute(CommandFactory.newBatchExecution(commands));
        
        LOG.debug("{}", check);
        return check.isGranted();
	}
	
	 private static class AgendaLogger extends DefaultAgendaEventListener {
         @Override
         public void afterActivationFired(AfterActivationFiredEvent event) {
        	 LOG.debug("RULE {} {}", event.getActivation().getRule().getName(), event.getActivation().getFactHandles());
         }
	 }
}
