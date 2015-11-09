package helpers.validators;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.data.validation.Check;

import com.google.common.base.Splitter;

public class StringIsTime extends Check {

	/**
	 * matches strings as HH:MM or HHMM
	 */
	@Override
	public boolean isSatisfied(Object validatedObject, Object time) {

		setMessage("invalid.time");
		
		return Pattern.compile("^(([0-1][0-9]|2[0-3]):?[0-5][0-9])$")
				.matcher((String)time).matches();
	}
}
