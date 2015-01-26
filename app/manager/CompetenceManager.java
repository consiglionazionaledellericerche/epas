package manager;

import helpers.ModelQuery.SimpleResults;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import play.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDayDao;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TotalOvertime;

public class CompetenceManager {
	
	/**
	 * 
	 * @return la lista di stringhe popolata con i codici dei vari tipi di straordinario prendibili
	 */
	public static List<String> populateListWithOvertimeCodes(){
		List<String> list = Lists.newArrayList();
		list.add("S1");
		list.add("S2");
		list.add("S3");
		return list;
	}

	/**
	 * 
	 * @param total
	 * @return il quantitativo di straordinari totali
	 */
	public static Integer getTotalOvertime(List<TotalOvertime> total){
		Integer totaleMonteOre = 0;
		for(TotalOvertime tot : total){			
			totaleMonteOre = totaleMonteOre+tot.numberOfHours;
		}
		return totaleMonteOre;
	}
	
	/**
	 * 
	 * @param competenceYearList
	 * @return il quantitativo su base annuale di straordinari
	 */
	public static int getTotalYearlyOvertime(List<Competence> competenceYearList){
		int totaleOreStraordinarioAnnuale = 0;
		for(Competence comp : competenceYearList){
			
			totaleOreStraordinarioAnnuale = totaleOreStraordinarioAnnuale + comp.valueApproved;
		}
		return totaleOreStraordinarioAnnuale;
	}
	
	/**
	 * 
	 * @param competenceMonthList
	 * @return il quantitativo su base mensile di straordinari
	 */
	public static int getTotalMonthlyOvertime(List<Competence> competenceMonthList){
		int totaleOreStraordinarioMensile = 0;
		for(Competence comp : competenceMonthList){
			
			totaleOreStraordinarioMensile = totaleOreStraordinarioMensile + comp.valueApproved;
		}
		return totaleOreStraordinarioMensile;
	}
	
	/**
	 * 
	 * @param competenceCodeId
	 * @param code
	 * @param description
	 * @param codeAtt
	 * @return true se è stato possibile inserire un codice di competenza, false altrimenti 
	 */
	public static boolean setNewCompetenceCode(Long competenceCodeId, String code, String description, String codeAtt){
		
		if(competenceCodeId == null){
			CompetenceCode c = new CompetenceCode();
			c.code = code;
			c.codeToPresence = codeAtt;
			c.description = description;
			
			
			CompetenceCode codeControl = CompetenceCodeDao.getCompetenceCodeByCode(code);
			if(codeControl == null){
				c.save();
				
				return true;
			}
			else{
				return false;
			}

		}
		else{
			CompetenceCode c = CompetenceCodeDao.getCompetenceCodeById(competenceCodeId);
			c.code = code;
			c.codeToPresence = codeAtt;
			c.description = description;
			c.save();
			return true;
		}
		
	}
	
	/**
	 * 
	 * @param year
	 * @param numeroOre
	 * @param officeId
	 * @return true se è stato possibile inserire un aggiornamento per le ore di straordinario totali per l'ufficio office nell'anno year
	 */
	public static boolean saveOvertime(Integer year, String numeroOre, Long officeId){
		Office office = OfficeDao.getOfficeById(officeId);
		TotalOvertime total = new TotalOvertime();
		LocalDate data = new LocalDate();
		total.date = data;
		total.year = data.getYear();
		total.office = office;

		try {
			if(numeroOre.startsWith("-")) {

				total.numberOfHours = - new Integer(numeroOre.substring(1, numeroOre.length()));
			}
			else if(numeroOre.startsWith("+")) {

				total.numberOfHours = new Integer(numeroOre.substring(1, numeroOre.length()));
			}
			else {				
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}		
		total.save();
		return true;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param page
	 * @param name
	 * @param office
	 * @param beginMonth
	 * @param simpleResults
	 * @param code
	 * @return la tabella formata da persone, dato e valore intero relativi ai quantitativi orari su orario di lavoro, straordinario,
	 * riposi compensativi per l'anno year e il mese month per le persone dell'ufficio office
	 */
	public static Table<Person, String, Integer> composeTableForOvertime(int year, int month, Integer page, 
			String name, Office office, LocalDate beginMonth, SimpleResults<Person> simpleResults, CompetenceCode code){
		
		ImmutableTable.Builder<Person, String, Integer> builder = ImmutableTable.builder();
		Table<Person, String, Integer> tableFeature = null;	
		List<Person> activePersons = simpleResults.paginated(page).getResults();		
	
		for(Person p : activePersons){
			Integer daysAtWork = 0;
			Integer recoveryDays = 0;
			Integer timeAtWork = 0;
			Integer difference = 0;
			Integer overtime = 0;
			
			List<PersonDay> personDayList = PersonDayDao.getPersonDayInPeriod(p, beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), false);
			for(PersonDay pd : personDayList){
				if(pd.stampings.size()>0)
					daysAtWork = daysAtWork +1;
				timeAtWork = timeAtWork + pd.timeAtWork;
				difference = difference +pd.difference;
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals("94"))
						recoveryDays = recoveryDays+1;
				}
			}			
			Optional<Competence> comp = CompetenceDao.getCompetence(p, year, month, code);
			if(comp.isPresent())
				overtime = comp.get().valueApproved;
			else
				overtime = 0;
			builder.put(p, "Giorni di Presenza", daysAtWork);
			builder.put(p, "Tempo Lavorato (HH:MM)", timeAtWork);
			builder.put(p, "Tempo di lavoro in eccesso (HH:MM)", difference);
			builder.put(p, "Residuo - rip. compensativi", difference-(recoveryDays*60));
			builder.put(p, "Residuo netto", difference-(overtime*60));
			builder.put(p, "Ore straordinario pagate", overtime);
			builder.put(p, "Riposi compens.", recoveryDays);
						
		}
		tableFeature = builder.build();
		return tableFeature;
	}
	
	/**
	 * 
	 * @param personList
	 * @return la tabella che contiene nelle righe le persone, nelle colonne le competenze e come valori i booleani che determinano se 
	 * per la persona è attiva la competenza comp rappresentata dalla stringa della descrizione della competenza stessa  
	 */
	public static Table<Person, String, Boolean> getTableForEnabledCompetence(List<Person> personList){
		ImmutableTable.Builder<Person, String, Boolean> builder = ImmutableTable.builder();
		Table<Person, String, Boolean> tableRecapCompetence = null;
		
		List<CompetenceCode> allCodeList = CompetenceCodeDao.getAllCompetenceCode();
		List<CompetenceCode> codeList = new ArrayList<CompetenceCode>();
		for(CompetenceCode compCode : allCodeList) {			
			if( compCode.persons.size() > 0 )
				codeList.add(compCode);			
		}			
		for(Person p : personList) {

			for(CompetenceCode comp : codeList){
				if(p.competenceCode.contains(comp)){
					builder.put(p, comp.description+'\n'+comp.code, true);
				}
				else{
					builder.put(p, comp.description+'\n'+comp.code, false);
				}
			}
		}
		
		tableRecapCompetence = builder.build();
		return tableRecapCompetence;
	}
	
	/**
	 * 
	 * @param competence
	 * @param competenceCode
	 * @return true se avviene correttamente il cambiamento della lista di competenze attive per la persona Person passata come parametro
	 */
	public static boolean saveNewCompetenceEnabledConfiguration(Map<String, Boolean> competence, List<CompetenceCode> competenceCode, Person person){
		for(CompetenceCode code : competenceCode){
			boolean value = false;
			if (competence.containsKey(code.code)) {
				value = competence.get(code.code);
				Logger.info("competence %s is %s",  code.code, value);
			}
			if (!value){
				if(person.competenceCode.contains(CompetenceCodeDao.getCompetenceCodeById(code.id)))
					person.competenceCode.remove(CompetenceCodeDao.getCompetenceCodeById(code.id));
				else
					continue;
			} else { 
				if(person.competenceCode.contains(CompetenceCodeDao.getCompetenceCodeById(code.id)))
					continue;
				else
					person.competenceCode.add(CompetenceCodeDao.getCompetenceCodeById(code.id));
			}
			
		}		
		person.save();
		return true;
	}
	
	/**
	 * 
	 * @param year
	 * @param personList
	 * @return il file contenente tutti gli straordinari effettuati dalle persone presenti nella lista personList nell'anno year
	 * @throws IOException
	 */
	public static FileInputStream getOvertimeInYear(int year, List<Person> personList) throws IOException{
		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Cognome Nome,Totale straordinari"+' '+year);
		out.newLine();
		List<CompetenceCode> codeList = Lists.newArrayList();
		codeList.add(CompetenceCodeDao.getCompetenceCodeByCode("S1"));
		for(Person p : personList){
			Long totale = null;
			Optional<Integer> result = CompetenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.<Integer>absent(), Optional.fromNullable(p), codeList);
			if(result.isPresent())
				totale = result.get().longValue();
		
			Logger.debug("Totale per %s %s vale %d", p.name, p.surname, totale);
			out.write(p.surname+' '+p.name+',');
			if(totale != null)			
				out.append(totale.toString());
			else
				out.append("0");
			out.newLine();
		}
		out.close();
		return inputStream;
	}
}
