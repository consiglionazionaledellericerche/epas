package controllers;

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

import models.Absence;
import models.Competence;
import models.Contract;
import models.Person;
import models.exports.PersonOvertime;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With( {Secure.class, RequestInit.class} )
public class Charts extends Controller{

	@Inject
	static SecurityRules rules;

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidual(Integer year, Integer month){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));

		List<Month> meseList = new ArrayList<Month>();

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

		if(params.get("yearChart") == null || params.get("monthChart") == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList, meseList);
		}

		year = params.get("yearChart", Integer.class);
		month = params.get("monthChart", Integer.class);
		List<Person> personeProva = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), true);
		List<PersonOvertime> poList = new ArrayList<PersonOvertime>();
		for(Person p : personeProva){
			if(p.office.equals(Security.getUser().get().person.office)){
				PersonOvertime po = new PersonOvertime();

				Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ? and c.month = ? and c.person = ?",
						"S1","S2","S3", year, month, p).first();

				Contract contract = p.getCurrentContract();
				CalcoloSituazioneAnnualePersona sit = new CalcoloSituazioneAnnualePersona(contract, year, new LocalDate(year,month,1));
				Mese mese = sit.getMese(year,month);
				po.month = month;
				po.year = year;
				po.overtimeHour = val;
				po.name = p.name;
				po.surname = p.surname;
				po.positiveHourForOvertime = mese.positiveResidualInMonth(p, year, month)/60;
				poList.add(po);
			}

		}
		render(poList, year, month, annoList, meseList);
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void indexCharts(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		render();
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void overtimeOnPositiveResidualInYear(Integer year){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));

		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}
		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);
		Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ?", 
				"S1","S2","S3", year).first();
		List<Person> personeProva = Person.getActivePersonsinYear(year, Security.getOfficeAllowed(), true);
		int totaleOreResidue = 0;
		for(Person p : personeProva){
			if(p.office.equals(Security.getUser().get().person.office)){
				for(int month=1; month<13;month++){
					//RTODO contratto attivo??
					Contract contract = p.getCurrentContract();
					CalcoloSituazioneAnnualePersona sit = new CalcoloSituazioneAnnualePersona(contract, year, new LocalDate(year,month,1).dayOfMonth().withMaximumValue());
					Mese mese = sit.getMese(year,month);
					totaleOreResidue = totaleOreResidue+(mese.positiveResidualInMonth(p, year, month)/60);
				}
				Logger.debug("Ore in più per %s %s nell'anno %d: %d", p.name, p.surname, year,totaleOreResidue);
			}

		}

		render(annoList, val, totaleOreResidue);

	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void whichAbsenceInYear(Integer year){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));

		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}

		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);

		Long missioniSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92").first();
		Long riposiCompensativiSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "91").first();
		Long malattiaSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "111").first();
		Long altreSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code not in(?,?,?)", 
				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92","91","111").first();

		Logger.debug("Missioni size: %d", missioniSize);
		Logger.debug("RiposiCompensativi size: %d", riposiCompensativiSize);
		Logger.debug("Malattia size: %d", malattiaSize);
		Logger.debug("Altre size: %d", altreSize);

		render(annoList, missioniSize, riposiCompensativiSize, malattiaSize, altreSize);

	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void checkLastYearAbsences(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		render();
	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void processLastYearAbsences(Blob file){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<RenderResult> listTrueFalse = new ArrayList<RenderResult>();
		List<RenderResult> listNull = new ArrayList<RenderResult>();

		if(file == null){
			renderText("E' una waterloo...");
		}

		try 
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(file.get()));
			String line = null;

			int indexMatricola = 0;
			int indexAssenza = 0;
			int indexDataAssenza = 0;
			//renderText("Numero colonne: "+numeroColonne+'\n'+"Indice Matricola: "+indexMatricola+'\n'+"Indice assenza: "+indexAssenza+'\n'+"Indice data assenza: "+indexDataAssenza+"");

			while((line = in.readLine()) != null) {

				if(line.contains("Query 3")){
					render(listTrueFalse, listNull);
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
				//tokens = line.split("\",\"");

				List<String> tokenList = Charts.splitter(line);

				//if(tokenList.size() != numeroColonne){
				//	renderResult = new RenderResult(line, null, null, null, null, null, true);
				//	list.add(renderResult);
				//	continue;
				//}
				try{
					int matricola = Integer.parseInt(Charts.removeApice(tokenList.get(indexMatricola)));
					String assenza = Charts.removeApice(tokenList.get(indexAssenza));
					LocalDate dataAssenza = Charts.buildDate(tokenList.get(indexDataAssenza));
					Person p = Person.find("Select p from Person p where p.number = ?", matricola).first();
					Absence abs = Absence.find("Select abs from Absence abs where abs.personDay.person = ? and abs.personDay.date = ?", 
							p, dataAssenza).first();
					if(abs == null){
						if(!dataAssenza.isBefore(new LocalDate(2013,1,1)))
							//							renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, false, "assenza prima della data inizio utilizzo del programma", null);
							//						else
							renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, false, "nessuna assenza trovata", null);

					}
					else{
						if(abs.absenceType.certificateCode.equalsIgnoreCase(assenza)){
							continue;
							//renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, true, null);
						}
						else{
							if(!abs.personDay.date.isBefore(new LocalDate(2013,1,1)))
								//								renderResult = new RenderResult(null, matricola, p.name, p.surname, assenza, dataAssenza, false, "assenza prima della data inizio utilizzo del programma", null);
								//							else
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
			render(listNull, listTrueFalse);
		}
		catch(Exception e)
		{
			Logger.warn("C'è del casino...");
		}
		render(listTrueFalse, listNull);
	}

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
		token = Charts.removeApice(token);
		token = token.substring(0, 10);
		String[] elements = token.split("/");
		LocalDate date = new LocalDate(Integer.parseInt(elements[2]),Integer.parseInt(elements[1]), Integer.parseInt(elements[0]));
		return date;
	}

	private static List<String> splitter(String line)
	{
		line = Charts.removeApice(line);
		List<String> list = new ArrayList<String>();
		boolean hasNext = true;
		while(hasNext)
		{
			if(line.contains("\",\""))
			{
				int index = line.indexOf("\",\"");
				String aux = Charts.removeApice(line.substring(0, index));
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


	private static class RenderResult{
		private String line;
		private Integer matricola;
		private String nome;
		private String cognome;
		private String codice;
		private LocalDate data;
		private boolean check;
		private String message;
		private String codiceInAnagrafica;

		private RenderResult(String line, Integer matricola, String nome, String cognome, String codice, LocalDate data, boolean check, String message, String codiceInAnagrafica){
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


	private static class Month{
		private int id;
		private String mese;

		private Month(int id, String mese){
			this.id = id;
			this.mese = mese;
		}
	}

	private static class Year{
		private int id;
		private int anno;

		private Year(int id, int anno){
			this.id = id;
			this.anno = anno;
		}
	}


	public static void exportHourAndOvertime(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = new ArrayList<Year>();
		annoList.add(new Year(1,2013));
		annoList.add(new Year(2,2014));
		annoList.add(new Year(3,2015));
		render(annoList);
	}


	public static void export(Integer year) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Person> personList = Person.getActivePersonsinYear(year, Security.getOfficeAllowed(), true);
		Logger.debug("Esporto dati per %s persone", personList.size());
		FileInputStream inputStream = null;
		File tempFile = File.createTempFile("straordinari"+year,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);
		Integer month = new LocalDate().getMonthOfYear();
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
			
			String situazione = p.surname+' '+p.name+',';
			
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(p.getCurrentContract(), year, new LocalDate(year,month,1).dayOfMonth().withMaximumValue());
			for(int i = 1; i <= month; i++){	
				
				Mese m = c.getMese(year, i);
				if(m != null){
					situazione = situazione+(new Integer(m.straordinariMinuti/60).toString())+','+(new Integer(m.riposiCompensativiMinuti/60).toString())+','+(new Integer(m.progressivoFinaleMese/60).toString())+',';
					totalOvertime = totalOvertime+new Integer(m.straordinariMinuti/60);
					totalCompensatoryRest = totalCompensatoryRest+new Integer(m.riposiCompensativiMinuti/60);
					totalPlusHours = totalPlusHours+new Integer(m.progressivoFinaleMese/60);
				}
					
				else
					situazione = situazione +("0"+','+"0"+','+"0");

			}
			out.append(situazione);
			out.append(new Integer(totalOvertime).toString()+',');
			out.append(new Integer(totalCompensatoryRest).toString()+',');
			out.append(new Integer(totalPlusHours).toString()+',');
			out.newLine();
			totalCompensatoryRest = 0;
			totalOvertime = 0;
			totalPlusHours = 0;

		}
		out.close();
		renderBinary(inputStream, "straordinariOreInPiuERiposiCompensativi"+year+".csv");
	}
}
