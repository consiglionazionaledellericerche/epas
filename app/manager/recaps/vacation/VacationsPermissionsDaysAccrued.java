package manager.recaps.vacation;

/**
 * Progressione maturazione ferie e permessi nell'arco dell'anno
 */
public class VacationsPermissionsDaysAccrued {

	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in 
	 * istituto da meno di tre anni
	 */
	public static int convertWorkDaysToVacationDaysLessThreeYears(int days){
		
		if(days<=0)
			return 0;
		
		if(days >= 1 && days <= 15)
			return 0;
		if(days >= 16 && days <= 45)
			return 2;
		if(days >= 46 && days <= 75)
			return 4;
		if(days >= 76 && days <= 106)
			return 6;
		if(days >= 107 && days <= 136)
			return 8;
		if(days >= 137 && days <= 167)
			return 10;
		if(days >= 168 && days <= 197)
			return 13;
		if(days >= 198 && days <= 227)
			return 15;
		if(days >= 228 && days <= 258)
			return 17;
		if(days >= 259 && days <= 288)
			return 19;
		if(days >= 289 && days <= 319)
			return 21;
		if(days >= 320 && days <= 349)
			return 23;
		
		else
			return 26;
		
	}

	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in
	 * istituto da più di tre anni
	 */
	public static int convertWorkDaysToVacationDaysMoreThreeYears(int days){
		if(days<=0)
			return 0;
		
		if(days >= 1 && days <= 15)
			return 0;
		if(days >= 16 && days <= 45)
			return 2;
		if(days >= 46 && days <= 75)
			return 4;
		if(days >= 76 && days <= 106)
			return 7;
		if(days >= 107 && days <= 136)
			return 9;
		if(days >= 137 && days <= 167)
			return 11;
		if(days >= 168 && days <= 197)
			return 14;
		if(days >= 198 && days <= 227)
			return 16;
		if(days >= 228 && days <= 258)
			return 18;
		if(days >= 259 && days <= 288)
			return 21;
		if(days >= 289 && days <= 319)
			return 23;
		if(days >= 320 && days <= 349)
			return 25;			
		else
			return 28;
		
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie maturati secondo il piano di accumulo
	 * previsto per il part time verticale
	 */
	public static int converWorkDaysToVacationDaysPartTime(int days){
		if(days<=0)
			return 0;
		
		if(days >= 1 && days <= 15)
			return 0;
		if(days >= 16 && days <= 45)
			return 2;
		if(days >= 46 && days <= 75)
			return 3;
		if(days >= 76 && days <= 106)
			return 5;
		if(days >= 107 && days <= 136)
			return 6;
		if(days >= 137 && days <= 167)
			return 8;
		if(days >= 168 && days <= 197)
			return 10;
		if(days >= 198 && days <= 227)
			return 12;
		if(days >= 228 && days <= 258)
			return 14;
		if(days >= 259 && days <= 288)
			return 15;
		if(days >= 289 && days <= 319)
			return 17;
		if(days >= 320 && days <= 349)
			return 18;			
		else
			return 21;
	}

	public static int converWorkDaysToVacationDaysPartTimeMoreThanThreeYears(int days){
		if(days<=0)
			return 0;
		if(days >= 1 && days <= 15)
			return 0;
		if(days >= 16 && days <= 45)
			return 2;
		if(days >= 46 && days <= 75)
			return 3;
		if(days >= 76 && days <= 106)
			return 6;
		if(days >= 107 && days <= 136)
			return 7;
		if(days >= 137 && days <= 167)
			return 9;
		if(days >= 168 && days <= 197)
			return 11;
		if(days >= 198 && days <= 227)
			return 13;
		if(days >= 228 && days <= 258)
			return 14;
		if(days >= 259 && days <= 288)
			return 17;
		if(days >= 289 && days <= 319)
			return 18;
		if(days >= 320 && days <= 349)
			return 20;			
		else
			return 22;
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
	
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di permesso maturati con il piano ferie
	 * relativo al part time
	 */
	public static int convertWorkDaysToPermissionDaysPartTime(int days){
		int permissionDays = 0;
		if(days >= 45 && days <= 135)
			permissionDays = 1;
		if(days >= 136 && days <= 315)
			permissionDays = 2;			
		if(days >= 316 && days <= 365)
			permissionDays = 3;
		return permissionDays;
	}
	
}