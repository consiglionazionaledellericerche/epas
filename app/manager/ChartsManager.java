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

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.Blob;
import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Absence;
import models.CompetenceCode;
import models.ConfGeneral;
import models.Contract;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.enumerate.ConfigurationFields;
import models.exports.PersonOvertime;
import models.rendering.VacationsRecap;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import controllers.Charts;
import controllers.Security;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ConfGeneralDao;
import dao.ContractDao;
import dao.PersonDao;

public class ChartsManager {
	
	/**Classi innestate che servono per la restituzione delle liste di anni e mesi per i grafici**/
	
	public final static class Month{
		private int id;
		private String mese;

		private Month(int id, String mese){
			this.id = id;
			this.mese = mese;
		}
	}

	public final static class Year{
		private int id;
		private int anno;

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
	
	public static class RenderResult{
		private String line;
		private Integer matricola;
		private String nome;
		private String cognome;
		private String codice;
		private LocalDate data;
		private boolean check;
		private String message;
		private String codiceInAnagrafica;

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
	public static List<Year> populateYearList(Office office){
		List<Year> annoList = Lists.newArrayList();
		Integer yearBegin = null;
		int counter = 0;
		Optional<ConfGeneral> yearInitUseProgram = ConfGeneralDao.getConfGeneralByField(ConfigurationFields.InitUseProgram.description, office);
		if(yearInitUseProgram.isPresent()){
			counter++;			
			LocalDate date = new LocalDate(yearInitUseProgram.get().fieldValue);
			yearBegin =  date.getYear();
			annoList.add(new Year(counter, yearBegin));
			
		}
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
	public static List<Month> populateMonthList(){
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
	
	/**
	 * 
	 * @return la lista dei competenceCode che comprende tutti i codici di straordinario presenti in anagrafica
	 */
	public static List<CompetenceCode> populateOvertimeCodeList(){
		List<CompetenceCode> codeList = Lists.newArrayList();
		CompetenceCode c1 = CompetenceCodeDao.getCompetenceCodeByCode("S1");
		CompetenceCode c2 = CompetenceCodeDao.getCompetenceCodeByCode("S2");
		CompetenceCode c3 = CompetenceCodeDao.getCompetenceCodeByCode("S3");
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
	public static List<PersonOvertime> populatePersonOvertimeList(List<Person> personList, List<CompetenceCode> codeList, int year, int month)
	{
		List<PersonOvertime> poList = Lists.newArrayList();
		for(Person p : personList){
			if(p.office.equals(Security.getUser().get().person.office)){
				PersonOvertime po = new PersonOvertime();
				Long val = null;				
				Optional<Integer> result = CompetenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.fromNullable(month), Optional.fromNullable(p), codeList);
				if (result.isPresent())
					val = result.get().longValue();

				po.month = month;
				po.year = year;
				po.overtimeHour = val;
				po.name = p.name;
				po.surname = p.surname;
				po.positiveHourForOvertime = PersonResidualMonthRecap.positiveResidualInMonth(p, year, month)/60;
				poList.add(po);
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
	public static int calculateTotalResidualHour(List<Person> personeProva, int year){
		int totaleOreResidue = 0;
		for(Person p : personeProva){
			if(p.office.equals(Security.getUser().get().person.office)){
				for(int month=1; month<13;month++){
					totaleOreResidue = totaleOreResidue+(PersonResidualMonthRecap.positiveResidualInMonth(p, year, month)/60);
				}
				Logger.debug("Ore in più per %s %s nell'anno %d: %d", p.name, p.surname, year,totaleOreResidue);
			}

		}
		return totaleOreResidue;
	}
	
	
	public static RenderList checkSituationPastYear(Blob file){
		if(file == null){
			Logger.error("E' una waterloo...");
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
				List<String> tokenList = ChartsManager.splitter(line);
	
				try{
					int matricola = Integer.parseInt(removeApice(tokenList.get(indexMatricola)));
					String assenza = removeApice(tokenList.get(indexAssenza));
					LocalDate dataAssenza = buildDate(tokenList.get(indexDataAssenza));
					Person p = PersonDao.getPersonByNumber(matricola);
					Absence abs = AbsenceDao.getAbsenceInDay(Optional.fromNullable(p), dataAssenza, Optional.<LocalDate>absent(), false).size() > 0 ? AbsenceDao.getAbsenceInDay(Optional.fromNullable(p), dataAssenza, Optional.<LocalDate>absent(), false).get(0) : null;
					
					if(abs == null){
						if(!dataAssenza.isBefore(new LocalDate(2013,1,1)))
							renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, false, "nessuna assenza trovata", null);
					}
					else{
						if(abs.absenceType.certificateCode.equalsIgnoreCase(assenza)){
							continue;							
						}
						else{
							if(!abs.personDay.date.isBefore(new LocalDate(2013,1,1)))
								renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, false, "assenza diversa da quella in anagrafica", abs.absenceType.code);
						}
					}

				}
				catch(Exception e){
					e.printStackTrace();
					renderResult = new RenderResult(line, null, null, null, null, null, true, null, null);
					listNull.add(renderResult);
					continue;
				}

				listTrueFalse.add(renderResult);

			}
			return new RenderList(listNull, listTrueFalse);
		}
		catch(Exception e)
		{
			Logger.warn("C'è del casino...");
		}
		return new RenderList(listTrueFalse, listNull);
	}

	/**
	 * 
	 * @param year
	 * @param personList
	 * @return il file contenente la situazione di ore in più, ore di straordinario e riposi compensativi per ciascuna persona della lista 
	 * passata come parametro relativa all'anno year
	 * @throws IOException
	 */
	public static FileInputStream export(Integer year, List<Person> personList) throws IOException{
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
			out.append("ore straordinari "+DateUtility.fromIntToStringMonth(i)+','+"ore riposi compensativi "+DateUtility.fromIntToStringMonth(i)+','+"ore in più "+DateUtility.fromIntToStringMonth(i)+',');
		}

		out.append("ore straordinari TOTALI,ore riposi compensativi TOTALI, ore in più TOTALI");
		out.newLine();

		int totalOvertime = 0;
		int totalCompensatoryRest = 0;
		int totalPlusHours = 0;
		
		for(Person p : personList){
			Logger.debug("Scrivo i dati per %s %s", p.name, p.surname);

			out.append(p.surname+' '+p.name+',');
			String situazione = "";
			List<Contract> contractList = PersonDao.getContractList(p, beginDate, endDate);

			LocalDate beginContract = null;
			if(contractList.isEmpty())
				contractList = p.contracts;
			
			for(Contract contract : contractList){
				if(beginContract != null && beginContract.equals(contract.beginContract)){
					Logger.debug("Due contratti uguali nella stessa lista di contratti per %s %s : come è possibile!?!?", p.name, p.surname);
				}
				else{
					beginContract = contract.beginContract;
					PersonResidualYearRecap c = PersonResidualYearRecap.factory(contract, year, contract.endContract);
					if(c != null){
						int start = c.getMesi().get(0).mese;
						for(int i = start; i <= c.getMesi().size(); i++){	

							PersonResidualMonthRecap m = c.getMese(i);
							if(m != null){

								situazione = situazione+(new Integer(m.straordinariMinuti/60).toString())+','+(new Integer(m.riposiCompensativiMinuti/60).toString())+','+(new Integer((m.progressivoFinalePositivoMese+m.straordinariMinuti)/60).toString())+',';
															
								totalOvertime = totalOvertime+new Integer(m.straordinariMinuti/60);
								totalCompensatoryRest = totalCompensatoryRest+new Integer(m.riposiCompensativiMinuti/60);
								totalPlusHours = totalPlusHours+new Integer((m.progressivoFinalePositivoMese+m.straordinariMinuti)/60);
								
							}

							else
								situazione = situazione +("0"+','+"0"+','+"0");

						}
						out.append(situazione);
						out.append(new Integer(totalOvertime).toString()+',');
						out.append(new Integer(totalCompensatoryRest).toString()+',');
						out.append(new Integer(totalPlusHours).toString()+',');				

					}				

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
	public static FileInputStream exportDataSituation(Person person) throws IOException{
		File tempFile = File.createTempFile("esportazioneSituazioneFinale"+person.surname,".csv" );
		FileInputStream inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);

		out.write("Cognome Nome,Ferie usate anno corrente,Ferie usate anno passato,Permessi usati anno corrente,Residuo anno corrente (minuti), Residuo anno passato (minuti),Riposi compensativi anno corrente");
		out.newLine();
		
		VacationsRecap vr = VacationsRecap.Factory.build(person, LocalDate.now().getYear(), Optional.<Contract>absent(), LocalDate.now(), false);
		
		PersonResidualYearRecap pryr = PersonResidualYearRecap.factory(ContractDao.getContract(LocalDate.now(), person), LocalDate.now().getYear(), LocalDate.now());
		PersonResidualMonthRecap prmr = pryr.getMese(LocalDate.now().getMonthOfYear());
		WorkingTimeType wtt = person.getCurrentWorkingTimeType();
		int workingTime = wtt.workingTimeTypeDays.get(0).workingTime;
		out.append(person.surname+' '+person.name+',');
		out.append(new Integer(vr.vacationDaysCurrentYearUsed.size()).toString()+','+
				new Integer(vr.vacationDaysLastYearUsed.size()).toString()+','+
				new Integer(vr.permissionUsed.size()).toString()+','+
				new Integer(prmr.monteOreAnnoCorrente).toString()+','+
				new Integer(prmr.monteOreAnnoPassato).toString()+',');
		int month = LocalDate.now().getMonthOfYear();
		int riposiCompensativiMinuti = 0;
		for(int i = 1; i <= month; i++){
			PersonResidualMonthRecap pm = pryr.getMese(i);
			riposiCompensativiMinuti+=pm.riposiCompensativiMinuti;
		}
		out.append(new Integer(riposiCompensativiMinuti/workingTime).toString());

		out.close();
		return inputStream;
	}
	
	
	/**Metodi privati per il calcolo da utilizzare per la restituzione al controller del dato richiesto**/
	
	private static String removeApice(String token)
	{
		if(token.startsWith("\""))
			token = token.substring(1);
		if(token.endsWith("\""))
			token = token.substring(0, token.length()-1);
		return token;
	}

	private static LocalDate buildDate(String token)
	{
		token = removeApice(token);
		token = token.substring(0, 10);
		String[] elements = token.split("/");
		LocalDate date = new LocalDate(Integer.parseInt(elements[2]),Integer.parseInt(elements[1]), Integer.parseInt(elements[0]));
		return date;
	}

	
	
	private static List<String> splitter(String line)
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
