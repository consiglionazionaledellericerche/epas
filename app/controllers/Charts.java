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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ChartsManager;
import manager.ChartsManager.Month;
import manager.ChartsManager.RenderList;
import manager.ChartsManager.RenderResult;
import manager.ChartsManager.Year;
import manager.recaps.PersonResidualMonthRecap;
import manager.recaps.PersonResidualYearRecap;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.exports.PersonOvertime;
import models.rendering.VacationsRecap;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.ContractDao;
import dao.PersonDao;
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
		
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);
		List<Month> meseList = ChartsManager.populateMonthList();	

		if(params.get("yearChart") == null || params.get("monthChart") == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList, meseList);
		}

		year = params.get("yearChart", Integer.class);
		month = params.get("monthChart", Integer.class);
		//List<Person> personeProva = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), true);
		List<Person> personeProva = PersonDao.list(Optional.<String>absent(), new HashSet(Security.getOfficeAllowed()), true, new LocalDate(year,month,1), new LocalDate(year, month,1).dayOfMonth().withMaximumValue(), true).list();
		
		
		List<CompetenceCode> codeList = ChartsManager.populateOvertimeCodeList();
		List<PersonOvertime> poList = ChartsManager.populatePersonOvertimeList(personeProva, codeList, year, month);
		
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
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);


		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}
		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);
		//Nuovo codice per la gestione delle query con queryDSL
		List<CompetenceCode> codeList = ChartsManager.populateOvertimeCodeList();
		Long val = null;
		Optional<Integer> result = CompetenceDao.valueOvertimeApprovedByMonthAndYear(year, Optional.<Integer>absent(), Optional.<Person>absent(), codeList);
		if(result.isPresent())
			val = result.get().longValue();
		//		Long val = Competence.find("Select sum(c.valueApproved) from Competence c where c.competenceCode.code in (?,?,?) and c.year = ?", 
//				"S1","S2","S3", year).first();
		//List<Person> personeProva = Person.getActivePersonsinYear(year, Security.getOfficeAllowed(), true);
		List<Person> personeProva = PersonDao.list(Optional.<String>absent(), new HashSet(Security.getOfficeAllowed()), true, new LocalDate(year,1,1), new LocalDate(year,12,31), true).list();
		int totaleOreResidue = ChartsManager.calculateTotalResidualHour(personeProva, year);

		render(annoList, val, totaleOreResidue);

	}

	//@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void whichAbsenceInYear(Integer year){

		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);


		if(params.get("yearChart") == null && year == null){
			Logger.debug("Params year: %s", params.get("yearChart", Integer.class));
			Logger.debug("Chiamato metodo con anno e mese nulli");
			render(annoList);
		}

		year = params.get("yearChart", Integer.class);
		Logger.debug("Anno preso dai params: %d", year);

		//Codice aggiunto per le queryDSL
		List<String> absenceCode = Lists.newArrayList();
		absenceCode.add("92");
		absenceCode.add("91");
		absenceCode.add("111");
		LocalDate beginYear = new LocalDate(year, 1,1);
		LocalDate endYear = beginYear.monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue();
		Long missioniSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "92");
		Long riposiCompensativiSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "91");
		Long malattiaSize = AbsenceDao.howManyAbsenceInPeriod(beginYear, endYear, "111");
		Long altreSize = AbsenceDao.howManyAbsenceInPeriodNotInList(beginYear, endYear, absenceCode);
//		Long missioniSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
//				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92").first();
//		Long riposiCompensativiSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
//				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "91").first();
//		Long malattiaSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code = ?", 
//				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "111").first();
//		Long altreSize = Absence.find("Select count(abs) from Absence abs where abs.personDay.date between ? and ? and abs.absenceType.code not in(?,?,?)", 
//				new LocalDate(year,1,1), new LocalDate(year,1,1).monthOfYear().withMaximumValue().dayOfMonth().withMaximumValue(), "92","91","111").first();

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
		
		RenderList render = ChartsManager.checkSituationPastYear(file);
		List<RenderResult> listTrueFalse = render.getListTrueFalse();
		List<RenderResult> listNull = render.getListNull();
		
		render(listTrueFalse, listNull);
	}




	public static void exportHourAndOvertime(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		List<Year> annoList = ChartsManager.populateYearList(Security.getUser().get().person.office);

		render(annoList);
	}


	public static void export(Integer year) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		//List<Person> personList = Person.getActivePersonsinYear(year, Security.getOfficeAllowed(), true);
		List<Person> personList = PersonDao.list(Optional.<String>absent(), new HashSet(Security.getOfficeAllowed()), true, new LocalDate(year,1,1), LocalDate.now(), true).list();
		Logger.debug("Esporto dati per %s persone", personList.size());
		FileInputStream inputStream = ChartsManager.export(year, personList);
		
		renderBinary(inputStream, "straordinariOreInPiuERiposiCompensativi"+year+".csv");
	}

	public static void exportFinalSituation(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		Set<Office> offices = Sets.newHashSet();
		offices.add(Security.getUser().get().person.office);
		String name = null;
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), false, LocalDate.now(), LocalDate.now(), true).list();
		render(personList);
	}

	public static void exportDataSituation(Long personId) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		FileInputStream inputStream = null;
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		File tempFile = File.createTempFile("esportazioneSituazioneFinale"+person.surname,".csv" );
		inputStream = new FileInputStream( tempFile );
		FileWriter writer = new FileWriter(tempFile, true);
		BufferedWriter out = new BufferedWriter(writer);

		out.write("Cognome Nome,Ferie usate anno corrente,Ferie usate anno passato,Permessi usati anno corrente,Residuo anno corrente (minuti), Residuo anno passato (minuti),Riposi compensativi anno corrente");
		out.newLine();
		//VacationsRecap vr = new VacationsRecap(person, LocalDate.now().getYear(), person.getContract(LocalDate.now()), LocalDate.now(), false);
		VacationsRecap vr = VacationsRecap.Factory.build(person, LocalDate.now().getYear(), Optional.<Contract>absent(), LocalDate.now(), false);
		//PersonResidualYearRecap pryr = PersonResidualYearRecap.factory(person.getContract(LocalDate.now()), LocalDate.now().getYear(), LocalDate.now());
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
		renderBinary(inputStream, "exportDataSituation"+person.surname+".csv");

	}
}
