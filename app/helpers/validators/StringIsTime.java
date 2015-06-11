package helpers.validators;

import java.util.List;

import play.data.validation.Check;

import com.google.common.base.Splitter;

public class StringIsTime extends Check {

	@Override
	public boolean isSatisfied(Object validatedObject, Object time) {

		setMessage("Orario non valido");
		if(time!=null){
			List<String> hourMinute = Splitter.on(":").trimResults().splitToList((String) time);
			if(hourMinute.size() == 2){
				if(StringIsNumber(hourMinute.get(0)) && StringIsNumber(hourMinute.get(1))){
					int hour = Integer.parseInt(hourMinute.get(0));
					int minute = Integer.parseInt(hourMinute.get(1));
					return (hour >= 0 && hour <=23) && (minute >= 0 && minute <=59);
				}
			}
		}
		return false;
	}

	private static boolean StringIsNumber(String string){
		if(string != null && !string.isEmpty()){
			for (int i=0; i<string.length(); i++){
				char c = string.charAt(i);
				if (!(c >= '0' && c <= '9')){
					return false;
				};
			}
			return true;
		}
		return false;
	}
}
