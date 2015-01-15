package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.CheckAbsenceInsert;
import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;

import manager.AbsenceManager;
import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.ConfYear;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.Qualification;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;
import models.rendering.VacationsRecap;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import play.Logger;
import play.data.validation.InFuture;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.libs.Mail;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import controllers.Resecure.NoCheck;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.AbsenceTypeGroupDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;
import dao.PersonShiftDayDao;
import dao.QualificationDao;


@With( {Resecure.class, RequestInit.class} )
public class Absences extends Controller{

	@Inject
	static SecurityRules rules;
			
	public static void absences(int year, int month) {
		Person person = Security.getUser().get().person;
		YearMonth yearMonth = new YearMonth(year,month);
		Map<AbsenceType,Integer> absenceTypeInMonth = 
				AbsenceTypeDao.getAbsenceTypeInPeriod(person,
						DateUtility.getMonthFirstDay(yearMonth), 
						Optional.fromNullable(DateUtility.getMonthLastDay(yearMonth)));

		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(absenceTypeInMonth, year, month, month_capitalized);
	}

	public static void absenceInMonth(String absenceCode, int year, int month){
		Person person = Security.getUser().get().person;
		YearMonth yearMonth = new YearMonth(year,month);
		
		List<Absence> absences = AbsenceDao.getAbsenceByCodeInPeriod(
				Optional.fromNullable(person), 
				Optional.fromNullable(absenceCode), 
				DateUtility.getMonthFirstDay(yearMonth), 
				DateUtility.getMonthLastDay(yearMonth),
				Optional.<JustifiedTimeAtWork>absent(),
				false, 
				true);
		
		List<LocalDate> dateAbsences = FluentIterable.from(absences)
				.transform(AbsenceManager.AbsenceToDate.INSTANCE).toList();

		render(dateAbsences, absenceCode);
	}

	@NoCheck
	public static void manageAbsenceCode(String name, Integer page){
		if(page==null)
			page = 0;
		
		SimpleResults<AbsenceType> simpleResults = AbsenceTypeDao.getAbsences(Optional.fromNullable(name));
		List<AbsenceType> absenceList = simpleResults.paginated(page).getResults();
		
		render(absenceList, name, simpleResults);
	}

	public static void insertAbsenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		List<Qualification> qualificationList = QualificationDao.getQualification(null, null, true);
		List<AbsenceTypeGroup> abtList = AbsenceTypeGroupDao.getAbsenceTypeGroup(null,true);
		List<JustifiedTimeAtWork> justifiedTimeAtWorkList = Lists.newArrayList(JustifiedTimeAtWork.values());
		List<AccumulationType> accumulationTypeList = Lists.newArrayList(AccumulationType.values());
		List<AccumulationBehaviour> accumulationBehaviourList = Lists.newArrayList(AccumulationBehaviour.values());

		render(qualificationList, abtList, justifiedTimeAtWorkList, accumulationTypeList, accumulationBehaviourList);
	}

	public static void saveAbsenceCode(
			@Required @Valid AbsenceType absenceType,
			@Required String jwt,
			@Required int qual,
			AbsenceTypeGroup abtg,
			String accBehaviour,
			String accType,
			String codiceSostituzione
			){
		
    	if (validation.hasErrors()){
    		flash.error(validation.errorsMap().toString());
    		// TODO modificare il template per visualizzare gli errori di validazione
    	    insertAbsenceCode();
    	}
    	
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.getByDescription(jwt);
		
		for(int i = 1; i <= qual; i++){
			Qualification q = QualificationDao.byQualification(i).orNull();
			absenceType.qualifications.add(q);
		}

		if(abtg.label != null && !abtg.label.isEmpty()){
			abtg.accumulationBehaviour = AccumulationBehaviour.getByDescription(accBehaviour);
			abtg.accumulationType = AccumulationType.getByDescription(accType);

			if(accBehaviour.equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation.description)){
				abtg.replacingAbsenceType = AbsenceTypeDao.getAbsenceTypeByCode(codiceSostituzione);
			}
			absenceType.absenceTypeGroup = abtg;
			abtg.save();
		}
		
		absenceType.save();
		flash.success("Inserito nuovo codice di assenza %s", absenceType.code);
		
		manageAbsenceCode(null, null);
	}

	public static void create(@Required Long personId, @Valid @Required LocalDate date) {
		
		if (validation.hasErrors()){
    		flash.error(validation.errorsMap().toString());
    	    render();
    	}
		
		Person person = PersonDao.getPersonById(personId);
		Preconditions.checkNotNull(person);
		
		rules.checkIfPermitted(person.office);
		
		Logger.debug("Insert absence called for personId = %s, %s", personId, date);
		List<AbsenceType> frequentAbsenceTypeList = AbsenceTypeDao.getFrequentTypes();
		List<AbsenceType> allCodes = AbsenceTypeDao.getAbsenceTypeFromEffectiveDate(date);

		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes);
	}

	public static void insert(@Required Person person, 
			@Required LocalDate dateFrom,
			LocalDate dateTo,
			@Required String absenceCode, 
			Blob file){
		
		AbsenceType absenceType = AbsenceTypeDao.getAbsenceTypeByCode(absenceCode);
		
		if(person == null){
			flash.error("id della persona non valido");
		}
		if(absenceType == null){
			flash.error("codice di assenza inesistente", absenceCode);
		}
	
		if(flash.contains("error")){
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
	
		rules.checkIfPermitted(person.office);
		
		//Ho dovuto implementare un involucro perchè quando richiamavo questo medoto da update il campo blob era null.
		AbsenceManager.insertAbsence(person, dateFrom,Optional.fromNullable(dateTo), 
				absenceType, Optional.fromNullable(file), Optional.<String>absent());
		
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());	
	}
	
	public static void editCode(@Required Long absenceCodeId){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		AbsenceType abt = AbsenceTypeDao.getAbsenceTypeById(absenceCodeId);
		
		List<Qualification> qualList = QualificationDao.getQualification(null, null, true);
		List<JustifiedTimeAtWork> justList = Lists.newArrayList(JustifiedTimeAtWork.values());
		List<AccumulationType> accType = Lists.newArrayList(AccumulationType.values());
		List<AccumulationBehaviour> behaviourType = Lists.newArrayList(AccumulationBehaviour.values());

		render(abt, justList, qualList, accType, behaviourType);
	}

	
	public static void updateCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
		//TODO: rimuovere tutti i params.get presenti nel metodo passandoli invece come parametri al metodo stesso
		AbsenceType absence = AbsenceTypeDao.getAbsenceTypeById(params.get("absenceTypeId", Long.class));
		//AbsenceType absence = AbsenceType.findById(params.get("absenceTypeId", Long.class));
		if(absence == null)
			notFound();
		Logger.debug("Il codice d'assenza da modificare è %s", absence.code);
		absence.description = params.get("descrizione");
		Logger.debug("Il valore di uso interno è: %s", params.get("usoInterno", Boolean.class));
		Logger.debug("Il valore di uso multiplo è: %s", params.get("usoMultiplo", Boolean.class));
		Logger.debug("Il valore di tempo giustificato è: %s", params.get("abt.justifiedTimeAtWork"));
		absence.internalUse = params.get("usoInterno", Boolean.class);		
		absence.multipleUse = params.get("usoMultiplo", Boolean.class);
		absence.consideredWeekEnd = params.get("weekEnd", Boolean.class);
		absence.validFrom = new LocalDate(params.get("inizio"));
		absence.validTo = new LocalDate(params.get("fine"));
		String justifiedTimeAtWork = params.get("abt.justifiedTimeAtWork");			
		absence.justifiedTimeAtWork = JustifiedTimeAtWork.getByDescription(justifiedTimeAtWork);

		for(int i = 1; i <= 10; i++){
			if(params.get("qualification"+i) != null){
				Qualification q = QualificationDao.getQualification(null, Optional.fromNullable(new Long(i)), false).get(0);
				//Qualification q = Qualification.findById(new Long(i));
				if(!absence.qualifications.contains(q))
					absence.qualifications.add(q);
			}
			else{
				Qualification q = QualificationDao.getQualification(null, Optional.fromNullable(new Long(i)), false).get(0);
				//Qualification q = Qualification.findById(new Long(i));
				if(absence.qualifications.contains(q))
					absence.qualifications.remove(q);
			}
		}


		absence.mealTicketCalculation = params.get("calcolaBuonoPasto", Boolean.class);
//		absence.ignoreStamping = params.get("ignoraTimbrature", Boolean.class);
		if(!params.get("gruppo").equals("")){
			absence.absenceTypeGroup.label = params.get("gruppo");
			absence.absenceTypeGroup.accumulationBehaviour = AccumulationBehaviour.getByDescription((params.get("abt.absenceTypeGroup.accumulationBehaviour")));
			absence.absenceTypeGroup.accumulationType = AccumulationType.getByDescription((params.get("abt.absenceTypeGroup.accumulationType")));
			absence.absenceTypeGroup.limitInMinute = params.get("limiteAccumulo", Integer.class);
			absence.absenceTypeGroup.minutesExcess = params.get("minutiEccesso", Boolean.class);
			String codeToReplace = params.get("codiceSostituzione");
			AbsenceTypeGroup abtg = AbsenceTypeGroupDao.getAbsenceTypeGroup(Optional.fromNullable(codeToReplace), false).get(0);
			//AbsenceTypeGroup abtg = AbsenceTypeGroup.find("Select abtg from AbsenceTypeGroup abtg where abtg.code = ?", codeToReplace).first();
			absence.absenceTypeGroup = abtg;
		}
		absence.save();

		flash.success("Modificato codice di assenza %s", absence.code);
		Absences.manageAbsenceCode(null, null);

	}


	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = AbsenceDao.getAbsenceById(absenceId);
		//Absence absence = Absence.findById(absenceId);
		if (absence == null) {
			notFound();
		}
		
		rules.checkIfPermitted(absence.personDay.person.office);
		
		LocalDate date = absence.personDay.date;
		List<AbsenceType> frequentAbsenceTypeList = AbsenceTypeDao.getFrequentTypes();
		//List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
		MainMenu mainMenu = new MainMenu(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
		List<AbsenceType> allCodes = AbsenceTypeDao.getAbsenceTypeFromEffectiveDate(absence.personDay.date);
		render(absence, frequentAbsenceTypeList, allCodes, mainMenu);				
	}
    
	public static void update(@Required Absence absence,@Valid LocalDate dateTo,
		@Required String absenceCode, Blob file, String mealTicket){
		
	    Preconditions.checkState(absence.isPersistent());
		
		rules.checkIfPermitted(absence.personDay.person.office);
		
		if(file != null && file.exists()){
			Logger.debug("ricevuto file di tipo: %s", file.type());
		}
		
		Person person = absence.personDay.person;
		LocalDate dateFrom =  absence.personDay.date;	
		
		AbsenceType absenceType = AbsenceTypeDao.getAbsenceTypeByCode(absenceCode);
		
		if(absenceType == null){
			flash.error("codice di assenza inesistente", absenceCode);
		}
		if(dateTo != null && dateTo.isBefore(dateFrom)){
			flash.error("Errore nell'inserimento del campo Fino A, inserire una data valida. Operazione annullata");
		}
		if(flash.contains("error")){
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
							
		int deleted = AbsenceManager.removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);
		
		if(deleted > 0){
			flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
		}
		
		AbsenceManager.insertAbsence(person, dateFrom, Optional.fromNullable(dateTo),
				absenceType,Optional.fromNullable(file), Optional.fromNullable(mealTicket));
		
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	}
				
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertPersonChildren(){
		int month = new LocalDate().getMonthOfYear();
		int year = new LocalDate().getYear();
		List<Person> personList = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		render(personList);
	}
	
	public static class AttachmentsPerCodeRecap {
		
		List<Absence> absenceSameType = new ArrayList<Absence>();
		
		public String getCode() {
			return absenceSameType.get(0).absenceType.code;
		}
		
	}
	
	
	public static void manageAttachmentsPerCode(Integer year, Integer month){
		
		rules.checkIfPermitted("");
		LocalDate beginMonth = new LocalDate(year, month, 1);
		
		//Prendere le assenze ordinate per tipo
		List<Absence> absenceList = AbsenceDao.getAbsenceInDay(Optional.<Person>absent(), beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), true);
//		List<Absence> absenceList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup is null and " +
//				"abs.personDay.date between ? and ? and abs.absenceFile is not null order by abs.absenceType.code", 
//				beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
	
		List<AttachmentsPerCodeRecap> attachmentRecapList = new ArrayList<AttachmentsPerCodeRecap>();
		AttachmentsPerCodeRecap currentRecap = new AttachmentsPerCodeRecap();
		AbsenceType currentAbt = null;
		
		for(Absence abs : absenceList) {
			
			if(currentAbt == null) {
				
				currentAbt = abs.absenceType;
			}
			else if( !currentAbt.code.equals(abs.absenceType.code) ) {
				
				//finalizza tipo
				if( currentRecap.absenceSameType.size() > 0 )		/* evitato con la query abs.absenceFile is not null */
					attachmentRecapList.add(currentRecap);
				currentRecap = new AttachmentsPerCodeRecap();
				//nuovo tipo
				currentAbt = abs.absenceType;
			}
			if(abs.absenceFile.get() != null) {
				
				currentRecap.absenceSameType.add(abs);
			}
		}
		
		//finalizza ultimo tipo
		if( currentRecap.absenceSameType.size() > 0 )
			attachmentRecapList.add(currentRecap);
		
		render(attachmentRecapList, year, month);
	}
	
	
	public static void downloadAttachment(long id){
		Logger.debug("Assenza con id: %d", id);
		Absence absence = AbsenceDao.getAbsenceById(id);   
		//Absence absence = Absence.findById(id);
		rules.checkIfPermitted(absence.personDay.person.office);
		notFoundIfNull(absence);
		response.setContentTypeIfNotSet(absence.absenceFile.type());
		Logger.debug("Allegato relativo all'assenza: %s", absence.absenceFile.getFile());
		renderBinary(absence.absenceFile.get(), absence.absenceFile.length());
	}

	public static void zipAttachment(String code, Integer year, Integer month) throws IOException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		FileOutputStream fos = new FileOutputStream("attachment"+'-'+code+".zip");
		ZipOutputStream zos = new ZipOutputStream(fos);
		
		
		List<Absence> absList = AbsenceDao.getAbsenceByCodeInPeriod(Optional.<Person>absent(),Optional.fromNullable(code), 
				new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), 
				Optional.<JustifiedTimeAtWork>absent(), true, false);
//		List<Absence> absList = Absence.find("Select abs from Absence abs where abs.absenceType.code = ? "
//				+ "and abs.personDay.date between ? and ? and abs.absenceFile is not null",
//				code, new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		byte[] buffer = new byte[1024];

		for(Absence abs : absList){
			try {
			
				FileInputStream fis = new FileInputStream(abs.absenceFile.getFile());
				
				zos.putNextEntry(new ZipEntry(abs.absenceFile.getFile().getName()));
				
				int length;
				while ((length = fis.read(buffer)) >= 0) {
				       zos.write(buffer, 0, length);
				}
				
				zos.closeEntry();
				
				fis.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}	
		
		zos.close();
			
		renderBinary(new File("attachment"+'-'+code+".zip"));
	}
	
	
	public static void manageAttachmentsPerPerson(Long personId, Integer year, Integer month){
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<Absence> personAbsenceListWithFile = new ArrayList<Absence>();
		List<Absence> personAbsenceList = AbsenceDao.getAbsenceInDay(Optional.fromNullable(person), new LocalDate(year, month,1), Optional.fromNullable(new LocalDate(year, month,1).dayOfMonth().withMaximumValue()), false);
//		List<Absence> personAbsenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
//				"and abs.personDay.date between ? and ?", 
//				person, new LocalDate(year, month,1), new LocalDate(year, month,1).dayOfMonth().withMaximumValue()).fetch();
		for(Absence abs : personAbsenceList){
			if (abs.absenceFile.get() != null){
				personAbsenceListWithFile.add(abs);
			}
		}
		render(personAbsenceListWithFile, year, month, person);
	}	
	
	public static void absenceInPeriod(Long personId){

		List<Person> personList = Person.getActivePersonsInDay(LocalDate.now(),
				Security.getOfficeAllowed(), false);
		if(personId == null)
			personId = Security.getUser().get().person.id;
		Person person = PersonDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			Stampings.personStamping(Security.getUser().get().person.id, new LocalDate().getYear(), new LocalDate().getMonthOfYear());
		}
		
		
		rules.checkIfPermitted(person.office);
		
		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		
		try {
			
			String dataInizio = params.get("dataInizio");
			String dataFine = params.get("dataFine");
			dateFrom = new LocalDate(dataInizio);
			dateTo = new LocalDate(dataFine);
		} catch (Exception e) {
			
			flash.error("Errore nell'inserimento dei parametri. Valorizzare correttamente data inizio e data fine secondo il formato aaaa-mm-dd");
			render(personId, personList);
		}
		
		List<Absence> missioni = new ArrayList<Absence>();
		List<Absence> ferie = new ArrayList<Absence>();
		List<Absence> riposiCompensativi = new ArrayList<Absence>();
		List<Absence> altreAssenze = new ArrayList<Absence>();
		
		List<Absence> absenceList = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
				Optional.<String>absent(), dateFrom, dateTo, Optional.fromNullable(JustifiedTimeAtWork.AllDay), false, false);
//		List<Absence> absenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
//				"and abs.personDay.date between ? and ? and abs.absenceType.justifiedTimeAtWork = ?", person, dateFrom, dateTo, JustifiedTimeAtWork.AllDay).fetch();

		for(Absence abs : absenceList){
			if(abs.absenceType.code.equals("92")){
				missioni.add(abs);
			}
			else if(abs.absenceType.code.equals("31") || abs.absenceType.code.equals("32") || abs.absenceType.code.equals("94")){
				ferie.add(abs);
			}
			else if(abs.absenceType.code.equals("91")){
				riposiCompensativi.add(abs);
			}
			else
				altreAssenze.add(abs);
		}
		render(personList, person, absenceList, dateFrom, dateTo, missioni, ferie, riposiCompensativi, altreAssenze, personId);

	}
	
}