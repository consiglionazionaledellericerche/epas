package models;

public class VacationsPermissionsDaysAccrued {

	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in 
	 * istituto da meno di tre anni
	 */
	public static int convertWorkDaysToVacationDaysLessThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days <= 15)
			vacationDays = 0;
		if(days >= 16 && days <= 45)
			vacationDays = 2;
		if(days >= 46 && days <= 75)
			vacationDays = 4;
		if(days >= 76 && days <= 106)
			vacationDays = 6;
		if(days >= 107 && days <= 136)
			vacationDays = 8;
		if(days >= 137 && days <= 167)
			vacationDays = 10;
		if(days >= 168 && days <= 197)
			vacationDays = 13;
		if(days >= 198 && days <= 227)
			vacationDays = 15;
		if(days >= 228 && days <= 258)
			vacationDays = 17;
		if(days >= 259 && days <= 288)
			vacationDays = 19;
		if(days >= 289 && days <= 319)
			vacationDays = 21;
		if(days >= 320 && days <= 349)
			vacationDays = 23;
		if(days >= 350 && days <= 365)
			vacationDays = 26;
		return vacationDays;
	}

	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in
	 * istituto da più di tre anni
	 */
	public static int convertWorkDaysToVacationDaysMoreThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days <= 15)
			vacationDays = 0;
		if(days >= 16 && days <= 45)
			vacationDays = 2;
		if(days >= 46 && days <= 75)
			vacationDays = 4;
		if(days >= 76 && days <= 106)
			vacationDays = 7;
		if(days >= 107 && days <= 136)
			vacationDays = 9;
		if(days >= 137 && days <= 167)
			vacationDays = 11;
		if(days >= 168 && days <= 197)
			vacationDays = 14;
		if(days >= 198 && days <= 227)
			vacationDays = 16;
		if(days >= 228 && days <= 258)
			vacationDays = 18;
		if(days >= 259 && days <= 288)
			vacationDays = 21;
		if(days >= 289 && days <= 319)
			vacationDays = 23;
		if(days >= 320 && days <= 349)
			vacationDays = 25;
		if(days >= 350 && days <= 365)
			vacationDays = 28;
		return vacationDays;
	}

	/**
	 * 
	 * @param days
	 * @return il numero di giorni di permesso legge spettanti al dipendente a seconda dei giorni di presenza
	 */
	public static int convertWorkDaysToPermissionDays(int days){
		int permissionDays = 0;
		if(days >= 45 && days <= 135)
			permissionDays = 1;
		if(days >= 136 && days <= 225)
			permissionDays = 2;
		if(days >= 226 && days <= 315)
			permissionDays = 3;
		if(days >= 316 && days <= 365)
			permissionDays = 4;
		return permissionDays;
	}

}
