package manager.response;

import java.util.List;

import org.joda.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.common.collect.Lists;

@Data @NoArgsConstructor
public class AbsenceInsertResponseList {

	private List<AbsenceInsertResponse> absences = Lists.newArrayList();
	private List<String> warnings = Lists.newArrayList();
	private List<LocalDate> datesInTrouble = Lists.newArrayList();
	
	private int totalAbsenceInsert = 0;
	private int absenceInReperibilityOrShift = 0;

	public void add(AbsenceInsertResponse response) {
		absences.add(response);
		if(response.isInsertSucceeded()){
			totalAbsenceInsert++;
		}
		if(response.isDayInReperibilityOrShift()){
			absenceInReperibilityOrShift++;
		}
	}

	public boolean hasWarningOrDaysInTrouble() {
		return !(warnings.isEmpty() && datesInTrouble.isEmpty());
	}

}
