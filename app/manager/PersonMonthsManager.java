package manager;

import com.google.common.base.Optional;
import dao.PersonMonthRecapDao;
import models.Person;
import models.PersonMonthRecap;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.util.List;

public class PersonMonthsManager {

	public final static class Insertable{
		private boolean result;
		private String message;

		public Insertable(boolean result, String message){
			this.result = result;
			this.message = message;

		}

		public String getMessage(){
			return this.message;
		}

		public boolean getResult(){
			return this.result;
		}
	}

	@Inject
	private PersonMonthRecapDao personMonthRecapDao;
	/**
	 * 
	 * @param pm
	 * @param approved
	 * @param value
	 * @param from
	 * @param to
	 */
	public void saveTrainingHours(PersonMonthRecap pm, boolean approved, Integer value, LocalDate from, LocalDate to){
		pm.hoursApproved = false;
		pm.trainingHours = value;
		pm.fromDate = from;
		pm.toDate = to;
		pm.save();
	}

	/**
	 * 
	 * @param begin
	 * @param end
	 * @param value
	 * @param beginDate
	 * @param endDate
	 * @return un Insertable che controlla se è possibile prendere i parametri passati alla funzione oppure se questi presentano
	 * dei problemi
	 */
	public Insertable checkIfInsertable(int begin, int end, Integer value, LocalDate beginDate, LocalDate endDate){
		Insertable rr = new Insertable(true, "");
		if(begin > end){
			rr.message = "La data di inizio del periodo di formazione non può essere successiva a quella di fine";
			rr.result = false;
		}
		if(value == null || value < 0 || value > 24*(endDate.getDayOfMonth()-beginDate.getDayOfMonth()+1)){
			rr.message = "Non sono valide le ore di formazione negative, testuali o che superino la quantità massima di ore nell'intervallo temporale inserito.";
			rr.result = false;
		}
		return rr;
	}

	/**
	 * 
	 * @param pmList
	 * @return un Insertable che verifica se esiste già un periodo contenente delle ore di formazione per la persona person
	 */
	public Insertable checkIfPeriodAlreadyExists(Person person, int year, int month, LocalDate beginDate, LocalDate endDate){
		List<PersonMonthRecap> pmList = personMonthRecapDao.getPersonMonthRecaps(person, year, month, beginDate, endDate);
		Insertable rr = new Insertable(true, "");
		if(pmList != null && pmList.size() > 0){
			rr.message = "Esiste un periodo di ore di formazione che contiene uno o entrambi i giorni specificati.";
			rr.result = false;

		}
		return rr;
	}

	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return un Insertable che verifica se le ore di formazione per anno e mese richieste sono già state inviate
	 */
	public Insertable checkIfAlreadySend(Person person, int year, int month){
		Insertable rr = new Insertable(true, "");
		List<PersonMonthRecap> list = personMonthRecapDao.getPersonMonthRecapInYearOrWithMoreDetails(person, year, Optional.fromNullable(month), Optional.fromNullable(new Boolean(true)));

		if(list.size() > 0){
			rr.message = "Impossibile inserire ore di formazione per il mese precedente poichè gli attestati per quel mese sono già stati inviati";
			rr.result = false;

		}
		return rr;
	}

	/**
	 * 
	 * @param personMonthId
	 * @return un Insertable che controlla se esiste nel database una entry con l'id passato come parametro per quelle ore di formazione
	 */
	public Insertable checkIfExist(PersonMonthRecap pm){
		Insertable rr = new Insertable(true, "");

		if(pm == null) {
			rr.message = "Ore di formazione non trovate. Operazione annullata."; 
			rr.result = false;			
		}
		return rr;
	}

}
