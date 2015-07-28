package manager;

import it.cnr.iit.epas.CheckMessage;

import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import play.Logger;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import dao.AbsenceDao;
import dao.WorkingTimeTypeDao;



/**
 * //FIXME questo manager andrebbe riscritto da ZERO................
 * 
 *
 */
public class AbsenceGroupManager {
	
	private final WorkingTimeTypeDao workingTimeTypeDao;
	private final AbsenceDao absenceDao;
	
	@Inject
	public AbsenceGroupManager(WorkingTimeTypeDao workingTimeTypeDao,
			AbsenceDao absenceDao) {
		this.workingTimeTypeDao = workingTimeTypeDao;
		this.absenceDao = absenceDao;
	}

	/**
	 * 
	 * @param absenceType
	 * @return true se è possibile prendere il codice di assenza in questione in base ai parametri di accumulo, false altrimenti 
	 */
	public CheckMessage checkAbsenceGroup(AbsenceType absenceType, Person person, LocalDate date) {
		CheckMessage check = null;
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.nothing)){
			check = canTakeAbsenceWithNoAccumulation(absenceType, person, date);
		}
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.noMoreAbsencesAccepted)){
			check = canTakeAbsenceWithNoMoreAbsencesAccepted(absenceType, person, date);
		}
		if(absenceType.absenceTypeGroup.accumulationBehaviour.equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation)){
			check = canTakeAbsenceWithReplacingCodeAndDecreasing(absenceType, person, date);
		}
		return check;

	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se si può prendere il codice di assenza passato, considerando che quel codice d'assenza ha un gruppo che non prevede l'accumulo
	 * di valori: in effetti bisognerebbe capire se abbia senso una cosa del genere visto che allora non esistono casi che possano verificare 
	 * la situazione opposta
	 */
	private CheckMessage canTakeAbsenceWithNoAccumulation(AbsenceType absenceType, Person person, LocalDate date) {


		return new CheckMessage(true, "E' possibile prendere il codice d'assenza", null);
	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se è possibile prendere il codice di assenza passato come parametro dopo aver controllato di non aver ecceduto in quantità
	 * nel periodo di tempo previsto dal tipo di accumulo e, nel caso, lo sostituisce con il codice di rimpiazzamento se arriva al limite 
	 * previsto per quel codice  
	 */
	private CheckMessage canTakeAbsenceWithReplacingCodeAndDecreasing(
			AbsenceType absenceType, Person person, LocalDate date) {


		int totalMinutesJustified = 0;
		List<Absence> absList = null;
		//trovo nella storia dei personDay l'ultima occorrenza in ordine temporale del codice di rimpiazzamento relativo al codice di assenza
		//che intendo inserire, di modo da fare i calcoli sulla possibilità di inserire quel codice di assenza da quel giorno in poi.
		Absence absence = absenceDao.getLastOccurenceAbsenceInPeriod(absenceType, person, Optional.fromNullable(new LocalDate(date.getYear(),1,1)), date);
		if(absence != null){

			int minutesExcess = minutesExcessPreviousAbsenceType(absenceType, person, date);

			if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.yearly)){

				if(absenceType.absenceTypeGroup.limitInMinute >  absenceType.justifiedTimeAtWork.minutesJustified + minutesExcess)
					/**
					 * in questo caso non si è arrivati a raggiungere il limite previsto per quella assenza oraria 
					 */
					return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
							"di rimpiazzamento", null);

				else{
					/**
					 * si è arrivati a raggiungere il limite, a questo punto esistono due possibilità:
					 * raggiunto il limite, si guarda se il codice di sostituzione, nella somma delle proprie occorrenze in ambito annuale, ha 
					 * raggiunto o meno il limite per esso previsto, se sì non si fa prendere il codice di assenza altrimenti si concede
					 */
					int totalReplacingAbsence = 0;
					List<Absence> replacingAbsenceList = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
							Optional.fromNullable(absenceType.absenceTypeGroup.replacingAbsenceType.code), 
							date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date, Optional.<JustifiedTimeAtWork>absent(), false, false);

					totalReplacingAbsence = replacingAbsenceList.size();
					if(absenceType.absenceTypeGroup.replacingAbsenceType.absenceTypeGroup.limitInMinute <= totalReplacingAbsence*absenceType.absenceTypeGroup.limitInMinute){
						return new CheckMessage(false,"Non è possibile prendere ulteriori assenze con questo codice poichè si è superato il limite massimo a livello annuale per il suo codice di rimpiazzamento", null);
					}
					else{
						return new CheckMessage(true, "Si può prendere il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
					}
				}

			}
			else if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.always)){

				if(absenceType.absenceTypeGroup.limitInMinute >  absenceType.justifiedTimeAtWork.minutesJustified + minutesExcess)
					/**
					 * in questo caso non si è arrivati a raggiungere il limite previsto per quella assenza oraria 
					 */
					return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
							"di rimpiazzamento", null);
				else{		
					return new CheckMessage(true, "Si può prendere il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
				}

			}
		}

		else{

			absList = absenceDao.getReplacingAbsenceOccurrenceListInPeriod(
					absenceType, person, new LocalDate(date.getYear(),1,1), date);

			for(Absence abs : absList){
				totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
			}
			if(totalMinutesJustified + absenceType.justifiedTimeAtWork.minutesJustified > absenceType.absenceTypeGroup.limitInMinute)
				return new CheckMessage(true, "Si può inserire il codice di assenza richiesto e viene inserito anche il codice di rimpiazzamento", absenceType.absenceTypeGroup.replacingAbsenceType);
			else
				return new CheckMessage(true, "Si può utilizzare il codice di assenza e non c'è necessità di rimpiazzare il codice con il codice " +
						"di rimpiazzamento", null);
		}


		return new CheckMessage(true, "Si può prendere il codice di assenza richiesto.", null);	
	}

	/**
	 * 
	 * @param absenceType
	 * @param person
	 * @param date
	 * @return true se è possibile prendere il codice d'assenza passato come parametro dopo aver controllato di non aver ecceduto in quantità
	 * nel periodo di tempo previsto dal tipo di accumulo
	 */
	private CheckMessage canTakeAbsenceWithNoMoreAbsencesAccepted(
			AbsenceType absenceType, Person person, LocalDate date) {

		int totalMinutesJustified = 0;
		List<Absence> absList = null;
		//controllo che il tipo di accumulo sia su base mensile cercando nel mese tutte le occorrenze di codici di assenza che hanno
		//lo stesso gruppo identificativo
		if(absenceType.absenceTypeGroup.accumulationType.equals(AccumulationType.monthly)){
			absList = absenceDao.getAllAbsencesWithSameLabel(absenceType, person, date.dayOfMonth().withMinimumValue(), date);

			Logger.debug("La lista di codici di assenza con gruppo %s contiene %d elementi", absenceType.absenceTypeGroup.label, absList.size());
			for(Absence abs : absList){
				totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
			}
			if(absenceType.absenceTypeGroup.limitInMinute >= totalMinutesJustified+absenceType.justifiedTimeAtWork.minutesJustified)
				return new CheckMessage(true, "E' possibile prendere il codice di assenza", null);
			else
				return new CheckMessage(false, "La quantità usata nell'arco del mese per questo codice ha raggiunto il limite. Non si può usarne un altro.", null);
		}
		//controllo che il tipo di accumulo sia su base annuale cercando nel mese tutte le occorrenze di codici di assenza che hanno
		//lo stesso gruppo identificativo
		else{
			absList = absenceDao.getReplacingAbsenceOccurrenceListInPeriod(
					absenceType, person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date);

			Logger.debug("List size: %d", absList.size());
			for(Absence abs : absList){
				if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay)
					totalMinutesJustified = workingTimeTypeDao
					.getWorkingTimeType(date, person).get()
					.workingTimeTypeDays.get(date.getDayOfWeek()-1).workingTime;
				else{

					totalMinutesJustified = totalMinutesJustified+abs.absenceType.justifiedTimeAtWork.minutesJustified;
				}


			}
			Logger.debug("TotalMinutesJustified= %d. Minuti giustificati: %d", totalMinutesJustified, absenceType.justifiedTimeAtWork.minutesJustified);
			int quantitaGiustificata;
			if(absenceType.justifiedTimeAtWork != JustifiedTimeAtWork.AllDay)
				quantitaGiustificata = absenceType.justifiedTimeAtWork.minutesJustified;
			else
				quantitaGiustificata = workingTimeTypeDao
				.getWorkingTimeType(date, person).get().workingTimeTypeDays
				.get(date.getDayOfWeek()-1).workingTime;

			if(absenceType.absenceTypeGroup.limitInMinute >= totalMinutesJustified+quantitaGiustificata)
				return new CheckMessage(true, "E' possibile prendere il codice di assenza", null);
			else
				return new CheckMessage(false, "La quantità usata nell'arco dell'anno per questo codice ha raggiunto il limite. Non si può usarne un altro.", null);
		}

	}

	/**
	 * 
	 * @param abt
	 * @param person
	 * @param date
	 * @return i minuti in eccesso, se ci sono, relativi all'inserimento del precedente codice di assenza dello stesso tipo 
	 */
	private int minutesExcessPreviousAbsenceType(AbsenceType abt, Person person, LocalDate date){

		//cerco l'ultima occorrenza del codice di completamento
		Absence absence = absenceDao.getLastOccurenceAbsenceInPeriod(abt, person, Optional.<LocalDate>absent(), date);

		if(absence == null)
			return 0;

		List<Absence> absList = absenceDao.getReplacingAbsenceOccurrenceListInPeriod(abt, person, new LocalDate(date.getYear(),1,1), date);

		int minutesExcess = 0;
		int minutesJustified = 0;
		for(Absence abs : absList){
			minutesJustified = minutesJustified + abs.absenceType.justifiedTimeAtWork.minutesJustified;
			if(minutesJustified + minutesExcess >= abs.absenceType.absenceTypeGroup.limitInMinute ){
				minutesExcess = minutesExcess + minutesJustified - abs.absenceType.absenceTypeGroup.limitInMinute;
				minutesJustified = 0;
			}
		}		

		return minutesExcess;
	}
}
