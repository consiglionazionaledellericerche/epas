package helpers;

import java.util.List;

import play.data.validation.Error;
import play.i18n.Messages;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

public class ValidationHelper {
	
	public static String errorsMessages(List<Error> errors){
		return FluentIterable.from(errors).limit(errors.size()-1)
				.transform(errorToString.ISTANCE).join(Joiner.on(" - "));
	}
	
    public enum errorToString implements Function<Error, String>{
		ISTANCE;

		@Override
		public String apply(Error input) {
			return Messages.get(input.getMessageKey(), input.getKey());
		}
	}

}
