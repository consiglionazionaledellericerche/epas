/**
 * 
 */
package controllers;

import java.util.List;

import models.Person;
import models.exports.AuthInfo;

import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author cristian
 *
 */
@With(Secure.class)
public class JsonExport extends Controller {

	//TODO: serve un permesso più specifico?
	@Check(Security.INSERT_AND_UPDATE_ADMINISTRATOR)
	public static void activePersons() {
		List<Person> activePersons = Person.getActivePersons(LocalDate.now());
		Logger.debug("activePersons.size() = %d", activePersons.size());
		List<AuthInfo> activeAuthInfos = FluentIterable.from(activePersons).transform(new Function<Person, AuthInfo>() {
			@Override
			public AuthInfo apply(Person person) {
				return new AuthInfo(person.username, person.password);
			}
		}).toImmutableList();
		
		renderJSON(activeAuthInfos);
	}
	
}
