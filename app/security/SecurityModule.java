package security;

import org.drools.KnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import models.User;
import play.mvc.Http;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import controllers.Security;

/**
 * Unione di injection con il play.
 * 
 * @author marco
 *
 */
public class SecurityModule implements Module {
	
	private final static Logger LOG = LoggerFactory.getLogger(SecurityModule.class);
	
	@Provides @Named("request.action")
	public String currentAction() {
		return Http.Request.current().action;
	}
	
	@Provides @Named("request.remoteAddress")
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
		LOG.debug("SecurityModule: currentOperator = " 
				+ (user.isPresent() ? user.get() : "non presente"));
		return Security.getUser();
	}

	@Override
	public void configure(Binder binder) {
		binder.bind(SecurityRules.class);
	}
}
