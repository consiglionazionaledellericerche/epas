package manager;

import it.cnr.iit.epas.DateUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Absence;
import models.CompetenceCode;
import models.ConfGeneral;
import models.Contract;
import models.ContractMonthRecap;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.enumerate.Parameter;
import models.exports.PersonOvertime;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.Blob;
import play.db.jpa.JPAPlugin;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import controllers.Security;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;

import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

public class ChartsManager {

	@Inject
	public ChartsManager(CompetenceCodeDao competenceCodeDao,
			CompetenceDao competenceDao,CompetenceManager competenceManager, 
			PersonDao personDao,VacationsRecapFactory vacationsFactory,
			AbsenceDao absenceDao, ConfGeneralManager confGeneralManager,
			IWrapperFactory wrapperFactory) {
		this.confGeneralManager = confGeneralManager;
		this.competenceCodeDao = competenceCodeDao;
		this.competenceDao = competenceDao;
		this.competenceManager = competenceManager;
		this.personDao = personDao;
		this.absenceDao = absenceDao;
		this.vacationsFactory = vacationsFactory;
		this.wrapperFactory = wrapperFactory;
	}

	private final static Logger log = LoggerFactory.getLogger(ChartsManager.class);
	
	private final ConfGeneralManager confGeneralManager;
	private final CompetenceCodeDao competenceCodeDao;
	private final CompetenceDao competenceDao;
	private final CompetenceManager competenceManager;
	private final PersonDao personDao;
	private final AbsenceDao absenceDao;
	private final VacationsRecapFactory vacationsFactory;
	private final IWrapperFactory wrapperFactory;


	/**Classi innestate che servono per la restituzione delle liste di anni e mesi per i grafici**/

	public final static class Month{
		public int id;
		public String mese;

		private Month(int id, String mese){
			this.id = id;
			this.mese = mese;
		}
	}

	public final static class Year{
		public int id;
		public int anno;

		private Year(int id, int anno){
			this.id = id;
			this.anno = anno;
		}
	}


	/**Classe per la restituzione di un oggetto al controller che contenga le liste per la verifica di quanto trovato all'interno
	del file dello schedone**/

	public final static class RenderList{
		private List<RenderResult> listNull;
		private List<RenderResult> listTrueFalse;

		private RenderList(List<RenderResult> listNull, List<RenderResult> listTrueFalse){
			this.listNull = listNull;
			this.listTrueFalse = listTrueFalse;
		}

		public List<RenderResult> getListNull(){
			return this.listNull;
		}
		public List<RenderResult> getListTrueFalse(){
			return this.listTrueFalse;

		}
	}

	/**classe privata per la restituzione del risultato relativo al processo di controllo sulle assenze dell'anno passato**/

	public class RenderResult{
		public String line;
		public Integer matricola;
		public String nome;
		public String cognome;
		public String codice;
		public LocalDate data;
		public boolean check;
		public String message;
		public String codiceInAnagrafica;

		public RenderResult(String line, Integer matricola, String nome, String cognome, String codice, LocalDate data, boolean check, String message, String codiceInAnagrafica){
			this.line = line;
			this.matricola = matricola;
			this.nome = nome;
			this.codice = codice;
			this.cognome = cognome;
			this.data = data;
			this.check = check;
			this.message = message;
			this.codiceInAnagrafica = codiceInAnagrafica;

		}
	}


	/**Inizio parte di business logic**/

	/**
	 * 
	 * @param office
	 * @return la lista di oggetti Year a partire dall'inizio di utilizzo del programma a oggi
	 */
	public List<Year> populateYearList(Office office){
		List<Year> annoList = Lists.newArrayList();
		Integer yearBegin = null;
		int counter = 0;

		ConfGeneral yearInitUseProgram = confGeneralManager.getConfGeneral(Parameter.INIT_USE_PROGRAM, office);
		counter++;			
		LocalDate date = new LocalDate(yearInitUseProgram.fieldValue);
		yearBegin =  date.getYear();
		annoList.add(new Year(counter, yearBegin));

		if(yearBegin != null){			
			for(int i = yearBegin; i <= LocalDate.now().getYear(); i++){
				counter++;
				annoList.add(new Year(counter,i));
			}
		}

		return annoList;
	}

	/**
	 * 
	 * @return la lista degli oggetti Month
	 */
	public List<Month> populateMonthList(){
		List<Month> meseList = Lists.newArrayList();
		meseList.add(new Month(1,"Gennaio"));
		meseList.add(new Month(2,"Febbraio"));
		meseList.add(new Month(3,"Marzo"));
		meseList.add(new Month(4,"Aprile"));
		meseList.add(new Month(5,"Maggio"));
		meseList.add(new Month(6,"Giugno"));
		meseList.add(new Month(7,"Luglio"));
		meseList.add(new Month(8,"Agosto"));
		meseList.add(new Month(9,"Settembre"));
		meseList.add(new Month(10,"Ottobre"));
		meseList.add(new Month(11,"Novembre"));
		meseList.add(new Month(12,"Dicembre"));
		return meseList;
	}

	/**.size()
	 * 
	 * @return la lista dei competenceCode che comprende tutti i codici di straordinario presenti in anagrafica
	 */
	public List<CompetenceCode> populateOvertimeCodeList(){
		List<CompetenceCode> codeList = Lists.newArrayList();
		CompetenceCode c1 = competenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode c2 = competenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode c3 = competenceCodeDao.getCompetenceCodeByCode("S3");
		codeList.add(c1);
		codeList.add(c2);
		codeList.add(c3);
		return codeList;
	}

	/**
	 * 
	 * @param personList
	 * @param codeList
	 * @param year
	 * @param month
	 * @return la lista dei personOvertime
	 */
	public List<PersonOvertime> populatePersonOvertimeList(List<Person> personList, List<CompetenceCode> codeList, int year, int month)
	{
		List<PersonOvertime> poList = Lists.newArrayList();
		for(Person p : personList){
			if(p.office.equals(Security.getUser().get().person.office)){
				PersonOvertime po = new PersonOvertime();
				Long val = null;				
				Optional<Integer> result = competenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.fromNullable(month), Optional.fromNullable(p), codeList);
				if (result.isPresent())
					val = result.get().longValue();

				po.month = month;
				po.year = year;
				po.overtimeHour = val;
				po.name = p.name;
				po.surname = p.surname;
				po.positiveHourForOvertime = competenceManager.positiveResidualInMonth(p, year, month)/60;
				poList.add(po);
				log.info("Aggiunto {} {} alla lista con i suoi dati", p.name, p.surname);
			}

		}
		return poList;
	}

	/**
	 * 
	 * @param personeProva
	 * @param year
	 * @return il totale delle ore residue per anno totali sommando quelle che ha ciascuna persona della lista personeProva
	 */
	public int calculateTotalResidualHour(List<Person> personeProva, int year){
		int totaleOreResidue = 0;
		for(Person p : personeProva){
			if(p.office.equals(Security.getUser().get().person.office)){
				for(int month=1; month<13;month++){
					totaleOreResidue = totaleOreResidue+(competenceManager.positiveResidualInMonth(p, year, month)/60);
				}
				log.debug("Ore in più per {} nell'anno {}: {}",
						new Object[] {p.getFullname(), year,totaleOreResidue});
			}

		}
		return totaleOreResidue;
	}


	public RenderList checkSituationPastYear(Blob file){
		if(file == null){
			log.error("file nullo nella chiamata della checkSituationPastYear");
		}

		List<RenderResult> listTrueFalse = new ArrayList<RenderResult>();
		List<RenderResult> listNull = new ArrayList<RenderResult>();
		try 
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(file.get()));
			String line = null;

			int indexMatricola = 0;
			int indexAssenza = 0;
			int indexDataAssenza = 0;

			while((line = in.readLine()) != null) {

				if(line.contains("Query 3")){
					return new RenderList(listTrueFalse, listNull);
				}
				if(line.contains("Query"))
				{					
					String[] tokens = line.split(",");

					for(int i = 0; i < tokens.length; i ++){
						if(tokens[i].startsWith("Matricola"))
							indexMatricola = i;
						if(tokens[i].startsWith("Codice Assenza"))
							indexAssenza = i;
						if(tokens[i].startsWith("Data Assenza"))
							indexDataAssenza = i;

					}
					continue;
				}
				RenderResult renderResult = null;				
				List<String> tokenList = splitter(line);

				try{
					int matricola = Integer.parseInt(removeApice(tokenList.get(indexMatricola)));
					String assenza = removeApice(tokenList.get(indexAssenza));
					LocalDate dataAssenza = buildDate(tokenList.get(indexDataAssenza));
					
					JPAPlugin.closeTx(false);
					JPAPlugin.startTx(false);
					
					Person p = personDao.getPersonByNumber(matricola);
					Absence abs = absenceDao.getAbsencesInPeriod(
							Optional.fromNullable(p), dataAssenza
							,Optional.<LocalDate>absent(), false).size() > 0 ? 
									absenceDao.getAbsencesInPeriod(
											Optional.fromNullable(p), dataAssenza
											, Optional.<LocalDate>absent(), false).get(0) : null;

											if(abs == null){
												if(!dataAssenza.isBefore
														(new LocalDate(LocalDate.now().getYear()-1,1,1)))
													renderResult = 
													new RenderResult(null, matricola, 
															p.name, p.surname, assenza, 
															dataAssenza, false, "nessuna assenza trovata",
															null);
												log.info("Nessuna assenza trovata in data {} per {}", dataAssenza, p.surname);
											}
											else{
												if(abs.absenceType.certificateCode
														.equalsIgnoreCase(assenza)){
													renderResult = new RenderResult(null, 
															matricola, p.name, p.surname, 
															assenza, dataAssenza, true, "", 
															null);
													log.info("Assenza riscontrata in data {} per {} con codice {}", dataAssenza, p.surname, assenza);
												}
												else{
													if(!abs.personDay.date.isBefore
															(new LocalDate(LocalDate.now().getYear()-1,1,1)))
														renderResult = new RenderResult(null, 
																matricola, p.name, p.surname, 
																assenza, dataAssenza, false, 
																"assenza diversa da quella in anagrafica", 
																abs.absenceType.code);
													log.info("Riscontrata assenza diversa da quella in anagrafica in data {} per {}", 
															dataAssenza, p.surname);
												}
											}
											

				}
				catch(Exception e){
					//e.printStackTrace();
					renderResult = new RenderResult(line, null, null, null, null, null, true, null, null);
					listNull.add(renderResult);
					continue;
				}
				if(renderResult.check == false) {
					listTrueFalse.add(renderResult);
					log.info("Inserito in lista render result per {} in data {} "
							+ "il codice {}", renderResult.cognome, renderResult.data, 
							renderResult.codice);
				}
				

			}

			return new RenderList(listNull, listTrueFalse);
		}
		catch(Exception e)
		{
			log.warn("C'è del casino...");
		}
		return new RenderList(listNull, listTrueFalse);
	}

	/**
	 * 
	 * @param year
	 * @param personList
	 * @return il file contenente la situazione di ore in più, ore di straordinario e riposi compensativi per ciascuna persona della lista 
	 * passata come parametro relativa all'anno year
	 * @throws IOException
	 */
	public FileInputStream export(Integer year, List<Person> personList) throws IOException{
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		FileInputStream inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		Integer month = null;
		LocalDate endDate = null; 
		LocalDate beginDate = null;
		if(year == new LocalDate().getYear()){
			month = new LocalDate().getMonthOfYear();
			endDate = new LocalDate().monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue();
			beginDate = new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue();
		}
		else{
			month = 12;
			endDate = new LocalDate(year,12,31);
			beginDate = new LocalDate(year,1,1);
		}
		out.write("Cognome Nome,");
		for(int i = 1; i <= month; i++){
			out.append("ore straordinari "+DateUtility.fromIntToStringMonth(i)+
					','+"ore riposi compensativi "+DateUtility.fromIntToStringMonth(i)+
					','+"ore in più "+DateUtility.fromIntToStringMonth(i)+',');
		}

		out.append("ore straordinari TOTALI,ore riposi compensativi TOTALI, ore in più TOTALI");
		out.newLine();

		int totalOvertime = 0;
		int totalCompensatoryRest = 0;
		int totalPlusHours = 0;

		for(Person p : personList){
			log.debug("Scrivo i dati per {}", p.getFullname());

			out.append(p.surname+' '+p.name+',');
			String situazione = "";
			List<Contract> contractList = personDao.getContractList(p, beginDate, endDate);

			LocalDate beginContract = null;
			if(contractList.isEmpty())
				contractList.addAll(p.contracts);

			for (Contract contract : contractList){
				if (beginContract != null && beginContract.equals(contract.beginContract)){
					log.error("Due contratti uguali nella stessa lista di contratti per {} : come è possibile!?!?", p.getFullname());
				
				} else {
					IWrapperContract c = wrapperFactory.create(contract);
					beginContract = contract.beginContract;
					YearMonth actual = new YearMonth(year,1);
					YearMonth last = new YearMonth(year,12);
					while (! actual.isAfter(last) ) {
						Optional<ContractMonthRecap> recap = c.getContractMonthRecap(actual);
						if(recap.isPresent()){
							situazione = situazione +
									(new Integer(recap.get().straordinariMinuti/60).toString())
									+','+(new Integer(recap.get().riposiCompensativiMinuti/60).toString())
									+','+(new Integer((recap.get().getPositiveResidualInMonth()
											+recap.get().straordinariMinuti)/60).toString())
									+',';
							totalOvertime = totalOvertime+new Integer(recap.get().straordinariMinuti/60);
							totalCompensatoryRest = totalCompensatoryRest+new Integer(recap.get().riposiCompensativiMinuti/60);
							totalPlusHours = totalPlusHours+new Integer((recap.get().getPositiveResidualInMonth()
									+recap.get().straordinariMinuti)/60);
						}
						else {
							situazione = situazione +("0"+','+"0"+','+"0");
						}
						actual = actual.plusMonths(1);
					}
					
					out.append(situazione);
					out.append(new Integer(totalOvertime).toString()+',');
					out.append(new Integer(totalCompensatoryRest).toString()+',');
					out.append(new Integer(totalPlusHours).toString()+',');				
				}				

			}		
			totalCompensatoryRest = 0;
			totalOvertime = 0;
			totalPlusHours = 0;
			out.newLine();
		}
		out.close();
		return inputStream;
	}

	/**
	 * 
	 * @param person
	 * @return la situazione in termini di ferie usate anno corrente e passato, permessi usati e residuo per la persona passata come parametro
	 * @throws IOException
	 */
	public FileInputStream exportDataSituation(Person person) throws IOException {
		File tempFile = File.createTempFile("esportazioneSituazioneFinale"+person.surname,".csv" );
		FileInputStream inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);

		out.write("Cognome Nome,Ferie usate anno corrente,Ferie usate anno passato,Permessi usati anno corrente,Residuo anno corrente (minuti), Residuo anno passato (minuti),Riposi compensativi anno corrente");
		out.newLine();

		IWrapperPerson wPerson = wrapperFactory.create(person);

		Optional<Contract> contract = wPerson.getCurrentContract();

		Preconditions.checkState(contract.isPresent());

		Optional<VacationsRecap> vr = vacationsFactory.create(LocalDate.now().getYear(),
				contract.get(), LocalDate.now(), false);
		
		Preconditions.checkState(vr.isPresent());

		Optional<ContractMonthRecap> recap = wrapperFactory.create(	contract.get())
				.getContractMonthRecap( new YearMonth(LocalDate.now()));

		if( !recap.isPresent() ) {
			out.close();
			return inputStream;
		}
		
		Optional<WorkingTimeType> wtt = wPerson.getCurrentWorkingTimeType();

		Preconditions.checkState(wtt.isPresent());

		int workingTime = wtt.get().workingTimeTypeDays.get(0).workingTime;
		out.append(person.surname+' '+person.name+',');
		out.append(new Integer(vr.get().vacationDaysCurrentYearUsed).toString()+','+
				new Integer(vr.get().vacationDaysLastYearUsed).toString()+','+
				new Integer(vr.get().permissionUsed).toString()+','+
				new Integer(recap.get().remainingMinutesCurrentYear).toString()+','+
				new Integer(recap.get().remainingMinutesLastYear).toString()+',');
		int month = LocalDate.now().getMonthOfYear();
		int riposiCompensativiMinuti = 0;
		for(int i = 1; i <= month; i++){
			recap = wrapperFactory.create(	contract.get())
					.getContractMonthRecap( new YearMonth(LocalDate.now().getYear(), i));
			if( recap.isPresent() ) {
				riposiCompensativiMinuti+=recap.get().riposiCompensativiMinuti;
			}
		}
		out.append(new Integer(riposiCompensativiMinuti/workingTime).toString());

		out.close();
		return inputStream;
	}


	/**Metodi privati per il calcolo da utilizzare per la restituzione al controller del dato richiesto**/

	private String removeApice(String token)
	{
		if(token.startsWith("\""))
			token = token.substring(1);
		if(token.endsWith("\""))
			token = token.substring(0, token.length()-1);
		return token;
	}

	private LocalDate buildDate(String token)
	{
		token = removeApice(token);
		token = token.substring(0, 10);
		String[] elements = token.split("/");
		LocalDate date = new LocalDate(Integer.parseInt(elements[2]),Integer.parseInt(elements[1]), Integer.parseInt(elements[0]));
		return date;
	}



	private List<String> splitter(String line)
	{
		line = removeApice(line);
		List<String> list = new ArrayList<String>();
		boolean hasNext = true;
		while(hasNext)
		{
			if(line.contains("\",\""))
			{
				int index = line.indexOf("\",\"");
				String aux = removeApice(line.substring(0, index));
				list.add(aux);
				line = line.substring(index+2, line.length()-1);
			}
			else
			{
				hasNext = false;
			}
		}
		return list;
	}

	/***********************************************************************************************************/

}
