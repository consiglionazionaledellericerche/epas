package manager.response;

import java.util.List;

import manager.response.AbsenceInsertResponse.toDate;

import org.joda.time.LocalDate;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import controllers.Wizard.WizardStep;

public class AbsenceInsertResponseList {
	
	private List<AbsenceInsertResponse> absences = Lists.newArrayList();
	private List<String> warnings = Lists.newArrayList();
	private List<LocalDate> datesInTrouble = Lists.newArrayList();
	private int totalAbsenceInsert = 0;
	private int absenceInReperibilityOrShift = 0;
	
	public void add(AbsenceInsertResponse response) {
		absences.add(response);
		if (response.isInsertSucceeded()) {
			totalAbsenceInsert++;
		}
		if (response.isDayInReperibilityOrShift()) {
			absenceInReperibilityOrShift++;
		}
	}
	
	public boolean hasWarningOrDaysInTrouble() {
		return !(warnings.isEmpty() && datesInTrouble.isEmpty());
	}

	public List<AbsenceInsertResponse> getAbsences() {
		return absences;
	}

	public void setAbsences(List<AbsenceInsertResponse> absences) {
		this.absences = absences;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public List<LocalDate> getDatesInTrouble() {
		return datesInTrouble;
	}

	public void setDatesInTrouble(List<LocalDate> datesInTrouble) {
		this.datesInTrouble = datesInTrouble;
	}

	public int getTotalAbsenceInsert() {
		return totalAbsenceInsert;
	}

	public void setTotalAbsenceInsert(int totalAbsenceInsert) {
		this.totalAbsenceInsert = totalAbsenceInsert;
	}

	public int getAbsenceInReperibilityOrShift() {
		return absenceInReperibilityOrShift;
	}

	public void setAbsenceInReperibilityOrShift(int absenceInReperibilityOrShift) {
		this.absenceInReperibilityOrShift = absenceInReperibilityOrShift;
	}
	
	public List<LocalDate> datesInReperibilityOrShift(){
		
		return FluentIterable.from(absences).filter(
				new Predicate<AbsenceInsertResponse>() {
		    	    @Override
		    	    public boolean apply(AbsenceInsertResponse air) {
		    	        return air.isDayInReperibilityOrShift();
		    	    }
		    	}).transform(AbsenceInsertResponse.toDate.INSTANCE).toList();
	}

}
