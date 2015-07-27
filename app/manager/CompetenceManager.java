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

import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TotalOvertime;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;


public class CompetenceManager {

	

	@Inject
	public CompetenceManager(CompetenceCodeDao competenceCodeDao,
			OfficeDao officeDao, CompetenceDao competenceDao,
			PersonDayDao personDayDao, IWrapperFactory wrapperFactory) {
		this.competenceCodeDao = competenceCodeDao;
		this.officeDao = officeDao;
		this.competenceDao = competenceDao;
		this.personDayDao = personDayDao;
		this.wrapperFactory = wrapperFactory;
	}

	private final static Logger log = LoggerFactory.getLogger(CompetenceManager.class);

	private final CompetenceCodeDao competenceCodeDao;
	private final OfficeDao officeDao;
	private final PersonDayDao personDayDao;
	private final CompetenceDao competenceDao;
	private final IWrapperFactory wrapperFactory;

	/**
	 * 
	 * @return la lista di stringhe popolata con i codici dei vari tipi di straordinario prendibili
	 */
	public List<String> populateListWithOvertimeCodes(){
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
	public Integer getTotalOvertime(List<TotalOvertime> total){
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
	public int getTotalYearlyOvertime(List<Competence> competenceYearList){
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
	public int getTotalMonthlyOvertime(List<Competence> competenceMonthList){
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
	public boolean setNewCompetenceCode(Long competenceCodeId, String code, String description, String codeAtt){

		if(competenceCodeId == null){
			CompetenceCode c = new CompetenceCode();
			c.code = code;
			c.codeToPresence = codeAtt;
			c.description = description;


			CompetenceCode codeControl = competenceCodeDao.getCompetenceCodeByCode(code);
			if(codeControl == null){
				c.save();

				return true;
			}
			else{
				return false;
			}

		}
		else{
			CompetenceCode c = competenceCodeDao.getCompetenceCodeById(competenceCodeId);
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
	 * @return true se è stato possibile inserire un aggiornamento per le ore 
	 * di straordinario totali per l'ufficio office nell'anno year
	 */
	public boolean saveOvertime(Integer year, String numeroOre, Long officeId){
		Office office = officeDao.getOfficeById(officeId);
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
	public Table<Person, String, Integer> composeTableForOvertime(int year, 
			int month, Integer page, 
			String name, Office office, LocalDate beginMonth, 
			SimpleResults<Person> simpleResults, CompetenceCode code){

		ImmutableTable.Builder<Person, String, Integer> builder = ImmutableTable.builder();
		Table<Person, String, Integer> tableFeature = null;	
		List<Person> activePersons = simpleResults.paginated(page).getResults();		

		for(Person p : activePersons){
			Integer daysAtWork = 0;
			Integer timeAtWork = 0;
			Integer difference = 0;
			Integer overtime = 0;

			List<PersonDay> personDayList = personDayDao.getPersonDayInPeriod(p, 
					beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()));
			for(PersonDay pd : personDayList){
				if(pd.stampings.size()>0)
					daysAtWork = daysAtWork +1;
				timeAtWork = timeAtWork + pd.timeAtWork;
				difference = difference +pd.difference;
			}			
			Optional<Competence> comp = competenceDao
					.getCompetence(p, year, month, code);
			if(comp.isPresent())
				overtime = comp.get().valueApproved;
			else
				overtime = 0;
			builder.put(p, "Giorni di Presenza", daysAtWork);
			builder.put(p, "Tempo Lavorato (HH:MM)", timeAtWork);
			builder.put(p, "Tempo di lavoro in eccesso (HH:MM)", difference);
			builder.put(p, "Ore straordinario pagate", overtime);


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
	public Table<Person, String, Boolean> getTableForEnabledCompetence(List<Person> personList){
		ImmutableTable.Builder<Person, String, Boolean> builder = ImmutableTable.builder();
		Table<Person, String, Boolean> tableRecapCompetence = null;

		List<CompetenceCode> allCodeList = competenceCodeDao.getAllCompetenceCode();
		List<CompetenceCode> codeList = new ArrayList<CompetenceCode>();
		for(CompetenceCode compCode : allCodeList) {			
			if( compCode.persons.size() > 0 )
				codeList.add(compCode);			
		}			
		if(codeList.size() == 0){
			for(Person p : personList){
				builder.put(p, "", false);
			}
			tableRecapCompetence = builder.build();
			return tableRecapCompetence;
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
	public boolean saveNewCompetenceEnabledConfiguration(Map<String, Boolean> competence,
			List<CompetenceCode> competenceCode, Person person){
		for(CompetenceCode code : competenceCode){
			boolean value = false;
			if (competence.containsKey(code.code)) {
				value = competence.get(code.code);
				log.info("competence {} is {}",  code.code, value);
			}
			if (!value){
				if(person.competenceCode
						.contains(competenceCodeDao.getCompetenceCodeById(code.id)))
					person.competenceCode
					.remove(competenceCodeDao.getCompetenceCodeById(code.id));
				else
					continue;
			} else { 
				if(person.competenceCode
						.contains(competenceCodeDao.getCompetenceCodeById(code.id)))
					continue;
				else
					person.competenceCode
					.add(competenceCodeDao.getCompetenceCodeById(code.id));
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
	public FileInputStream getOvertimeInYear(int year, List<Person> personList) throws IOException{
		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Cognome Nome,Totale straordinari"+' '+year);
		out.newLine();
		List<CompetenceCode> codeList = Lists.newArrayList();
		codeList.add(competenceCodeDao.getCompetenceCodeByCode("S1"));
		for(Person p : personList){
			Long totale = null;
			Optional<Integer> result = competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.<Integer>absent(), Optional.fromNullable(p), codeList);
			if(result.isPresent())
				totale = result.get().longValue();

			log.debug("Totale per {} vale %d", p.getFullname(), totale);
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

	/**
	 * Ritorna il numero di ore disponibili per straordinari per la persona nel mese.
	 * Calcola il residuo positivo del mese per straordinari inerente il contratto attivo nel mese.
	 * Nel caso di due contratti attivi nel mese viene ritornato il valore per il contratto più recente.
	 * Nel caso di nessun contratto attivo nel mese viene ritornato il valore 0.
	 * @param person
	 * @param year
	 * @param month
	 */
	public Integer positiveResidualInMonth(Person person, int year, int month){

		List<Contract> monthContracts = wrapperFactory
				.create(person).getMonthContracts(year, month);
		
		for(Contract contract : monthContracts) {
			
			IWrapperContract wContract = wrapperFactory.create(contract);

			if(wContract.isLastInMonth(month, year)) {
				
				Optional<ContractMonthRecap> recap = 
						wContract.getContractMonthRecap( new YearMonth(year, month));
				if(recap.isPresent()) {
					return recap.get().getPositiveResidualInMonth();
				}
			}
		}
		return 0;
	}

	/**
	 * La lista dei codici competenza attivi per le persone nell'anno
	 * 
	 * @param year
	 * @return
	 */
	public List<CompetenceCode> activeCompetence(int year){

		List<CompetenceCode> competenceCodeList = Lists.newArrayList();

		List<Competence> competenceList = 
				competenceDao.getCompetenceInYear(year, Optional.<Office>absent());

		for(Competence comp : competenceList){
			if(!competenceCodeList.contains(comp.competenceCode))
				competenceCodeList.add(comp.competenceCode);
		}
		return competenceCodeList;
	}
}
