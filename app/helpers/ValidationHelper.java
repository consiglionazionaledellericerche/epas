package helpers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import play.data.validation.Error;

import java.util.List;


/**
 * @author daniele
 * Classe di utilita' per visualizzare gli errori di validazione nel flash scope
 * recupera il nome e il messaggio di validazione dai Messages
 */
// FIXME implementare la validazione nei modali oppure rimuovere i modali per le form
public class ValidationHelper {

	public static String errorsMessages(List<Error> errors){

		return FluentIterable.from(errors).filter(new Predicate<Error>() {
			@Override
			public boolean apply(Error input) {
				return !input.message().equals("Validation failed");
			}}).transform(errorToString.ISTANCE).join(Joiner.on(";  "));
	}

	public enum errorToString implements Function<Error, String>{
		ISTANCE;
		@Override
		public String apply(Error input) {
			return input.getKey()+":"+input.message();
		}
	}

}
