/**
 * 
 */
package models.enumerate;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * Tipologie di tempo di lavoro giustificato o assegnato
 * Utilizzate nelle AbsenceType per modellare le diverse
 * tipologie. 
 * 
 * @author cristian
 * @author dario
 */
public enum TimeAtWorkModifier {
	
	/*   VALORI DI GIUSTIFICAZIONE ORARIA */
	JustifyAllDay(null,true),
	JustifyHalfDay(null,true),
	JustifyOneHour(60,true),
	JustifyTwoHours(120,true),
	JustifyThreeHours(180,true),
	JustifyFourHours(240,true),
	JustifyFiveHours(300,true),
	JustifySixHours(360,true),	
	JustifySevenHours(420,true),
	JustifyEightHours(480,true),
	JustifyNothing(0,true),
	JustifyTimeToComplete(null,true),
	JustifyReduceWorkingTimeOfTwoHours(null,true),
	
	/*   VALORI DI ASSEGNAMENTO ORARIO */
	AssignAllDay(null,false),
	AssignOneHour(60,false),
	AssignTwoHours(120,false),
	AssignThreeHours(180,false),
	AssignFourHours(240,false),
	AssignFiveHours(300,false),
	AssignSixHours(360,false),	
	AssignSevenHours(420,false),
	AssignNothing(0,false);
	

	public Integer minutes;
//	justify: TRUE giustifica l'orario specificato; FALSE  lo assegna
	public boolean justify;

	
	private TimeAtWorkModifier(Integer minutes,boolean justify) {
		this.justify = justify;
		this.minutes = minutes;
	}
	
	public boolean isFixedJustifiedTime() {
		return minutes != null;
	}
	
	public static Set<TimeAtWorkModifier> justifyingValues(){
		
		return FluentIterable.from(EnumSet.allOf(TimeAtWorkModifier.class))
				.filter(new Predicate<TimeAtWorkModifier>() {
					@Override
					public boolean apply(TimeAtWorkModifier input) {
						return input.justify;
					}}).toSet();
	}
	
	public static Set<TimeAtWorkModifier> assigningValues(){
		
		return FluentIterable.from(EnumSet.allOf(TimeAtWorkModifier.class))
				.filter(new Predicate<TimeAtWorkModifier>() {
					@Override
					public boolean apply(TimeAtWorkModifier input) {
						return !input.justify;
					}}).toSet();
	}
	
}
