package manager;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.TimeAtWorkModifier;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;

public class YearlyAbsencesManager {

	private final static Logger log = LoggerFactory.getLogger(YearlyAbsencesManager.class);
	@Inject
	private AbsenceDao absenceDao;

	/*Non è molto chiaro cosa facesse questa classe innestata all'interno di YearlyAbsences*/
	public final static class AbsenceTypeDays{
		public String absenceCode;
		public Integer i;

		public AbsenceTypeDays(String absenceCode, Integer i){
			this.absenceCode = absenceCode;
			this.i = i;
		}

		public AbsenceTypeDays(String absenceCode){
			this.absenceCode = absenceCode;
			this.i = null;
		}

		public AbsenceTypeDays(){
			this.absenceCode = null;
			this.i = null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((absenceCode == null) ? 0 : absenceCode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbsenceTypeDays other = (AbsenceTypeDays) obj;
			if (absenceCode == null) {
				if (other.absenceCode != null)
					return false;
			} else if (!absenceCode.equals(other.absenceCode))
				return false;
			return true;
		}

	}

	/*Così come non è chiaro cosa ci facesse questa...*/
	public final static class AbsenceTypeDate{
		public AbsenceType absenceType;
		public LocalDate date;

		public AbsenceTypeDate(AbsenceType absenceType, LocalDate date){
			this.absenceType = absenceType;
			this.date = date;
		}
	}

	public Comparator<Person> PersonNameComparator = new Comparator<Person>() {

		public int compare(Person person1, Person person2) {

			String name1 = person1.surname.toUpperCase();
			String name2 = person2.surname.toUpperCase();

			if(name1.equals(name2))
				return person1.name.toUpperCase().compareTo(person2.name.toUpperCase());
			return name1.compareTo(name2);

		}

	};	

	public Comparator<AbsenceType> AbsenceCodeComparator = new Comparator<AbsenceType>(){

		public int compare(AbsenceType absenceCode1, AbsenceType absenceCode2){
			return absenceCode1.code.compareTo(absenceCode2.code);

		}		

	};

	/**
	 * 
	 * @param persons
	 * @param abt
	 * @param begin
	 * @param end
	 * @return
	 */
	public Table<Person, AbsenceType, Integer> populateMonthlyAbsencesTable(List<Person> persons, AbsenceType abt, LocalDate begin, LocalDate end){
		Table<Person, AbsenceType, Integer> tableMonthlyAbsences = TreeBasedTable.create(PersonNameComparator, AbsenceCodeComparator);
		for(Person p : persons){
			List<Absence> absenceInMonth = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(p), Optional.<String>absent(), begin, end, Optional.<TimeAtWorkModifier>absent(), false, false);

			tableMonthlyAbsences.put(p, abt, absenceInMonth.size());
			for(Absence abs : absenceInMonth){
				Integer value = tableMonthlyAbsences.row(p).get(abs.absenceType);
				log.debug("Per la persona {} il codice {} vale: {}",
						new Object[]{p, abs.absenceType.code, value});
				if(value == null){
					log.debug("Inserisco in tabella nuova assenza per {} con codice {}", p, abs.absenceType.code);
					tableMonthlyAbsences.row(p).put(abs.absenceType, 1);
				}
				else{
					tableMonthlyAbsences.row(p).put(abs.absenceType, value+1);
					log.debug("Incremento il numero di giorni per l'assenza {} di {} al valore {}", 
							new Object[]{abs.absenceType.code, p, value+1});

				}
			}
		}
		return tableMonthlyAbsences;
	}
}
