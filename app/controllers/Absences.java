package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateUtility;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import manager.AbsenceManager;
import manager.response.AbsenceInsertReport;
import manager.response.AbsencesResponse;
import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Person;
import models.PersonDay;
import models.Qualification;
import models.User;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import play.Logger;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.AbsenceTypeGroupDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;


@With( {Resecure.class, RequestInit.class} )
public class Absences extends Controller{

	@Inject
	static SecurityRules rules;
			
	public static void absences(int year, int month) {
		Person person = Security.getUser().get().person;
		YearMonth yearMonth = new YearMonth(year,month);
		Map<AbsenceType,Long> absenceTypeInMonth = 
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
			@Required @Min(1) @Max(10) int qual,
			AbsenceTypeGroup abtg,
			String accBehaviour,
			String accType,
			String codiceSostituzione
			){
		
    	if (validation.hasErrors()){
    		flash.error(validation.errorsMap().toString());
//			TODO modificare il template per visualizzare gli errori di validazione
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
		Logger.info("Inserito nuovo codice di assenza %s", absenceType.code);
		flash.success("Inserito nuovo codice di assenza %s", absenceType.code);
		
		manageAbsenceCode(null, null);
	}

	public static void create(@Required Long personId, @Valid @NotNull LocalDate date) {
		
		if (validation.hasErrors()){
    		flash.error(validation.errors().toString());
    	    render();
    	}
		
		Person person = PersonDao.getPersonById(personId);
		Preconditions.checkNotNull(person);
		
		rules.checkIfPermitted(person.office);
		
		List<AbsenceType> frequentAbsenceTypeList = AbsenceTypeDao.getFrequentTypes();
		List<AbsenceType> allCodes = AbsenceTypeDao.getAbsenceTypeFromEffectiveDate(date);

		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes);
	}

	public static void insert(@Required Person person, 
			@NotNull LocalDate dateFrom,
			LocalDate dateTo,
			@Required String absenceCode, 
			Blob file){

		AbsenceType absenceType = AbsenceTypeDao.getAbsenceTypeByCode(absenceCode);
		
		Verify.verify(person.isPersistent(),"Persona specificata non esistente!");
		Verify.verifyNotNull(absenceType, "Codice di assenza %s inesistente!", absenceCode);
	
		rules.checkIfPermitted(person.office);
		
		AbsenceInsertReport air = AbsenceManager.insertAbsence(person, dateFrom,Optional.fromNullable(dateTo), 
				absenceType, Optional.fromNullable(file), Optional.<String>absent());

//		Verifica errori generali nel periodo specificato
		if(air.hasWarningOrDaysInTrouble()){
			
			flash.error(String.format(air.getWarnings().iterator().next() + 
					" - %s",air.getDatesInTrouble()));
		}
		
//		Verifica degli errori sui singoli giorni
		if(air.getTotalAbsenceInsert() == 0 && !air.getAbsences().isEmpty()){
			
			Multimap<String, LocalDate> errors = ArrayListMultimap.create();
			
			for(AbsencesResponse ar : air.getAbsences()){
				errors.put(ar.getWarning() + " [codice: " + ar.getAbsenceCode() + "]", ar.getDate());
			}

			flash.error(errors.toString());
		}

//		Verifica per eventuali giorni di reperibilità
		if(air.getAbsenceInReperibilityOrShift() > 0){
			flash.error("Attenzione! verificare le reperibilità nei seguenti giorni : %s", air.datesInReperibilityOrShift());
		}
			
		if(air.getTotalAbsenceInsert() > 0){
			flash.success("Inserite %s assenze con codice %s", 
					air.getTotalAbsenceInsert(),air.getAbsences().iterator().next().getAbsenceCode());
		}

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
				Qualification q = QualificationDao.getQualification(Optional.fromNullable(new Integer(i)), Optional.fromNullable(new Long(i)), false).get(0);
				//Qualification q = Qualification.findById(new Long(i));
				if(!absence.qualifications.contains(q))
					absence.qualifications.add(q);
			}
			else{
				Qualification q = QualificationDao.getQualification(Optional.<Integer>absent(), Optional.fromNullable(new Long(i)), false).get(0);
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
		Logger.info("Modificato codice di assenza %s", absence.code);

		flash.success("Modificato codice di assenza %s", absence.code);
		Absences.manageAbsenceCode(null, null);

	}


	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = AbsenceDao.getAbsenceById(absenceId);
		
		Verify.verify(absence != null,"Assenza specificata inesistente!");
		
		rules.checkIfPermitted(absence.personDay.person.office);
		
		List<AbsenceType> frequentAbsenceTypeList = AbsenceTypeDao.getFrequentTypes();
		List<AbsenceType> allCodes = AbsenceTypeDao.getAbsenceTypeFromEffectiveDate(absence.personDay.date);
		
		render(absence, frequentAbsenceTypeList, allCodes);				
	}
    
	public static void update(@Required Absence absence,@Valid LocalDate dateTo,
		@Required String absenceCode, Blob file, String mealTicket){
		
		Verify.verify(absence.isPersistent(),"Assenza specificata inesistente!");
						
		rules.checkIfPermitted(absence.personDay.person.office);
		
		if(file != null && file.exists()){
			Logger.debug("ricevuto file di tipo: %s", file.type());
		}
		
		Person person = absence.personDay.person;
		LocalDate dateFrom =  absence.personDay.date;	
			
		if(dateTo != null && dateTo.isBefore(dateFrom)){
			flash.error("Errore nell'inserimento del campo Fino a, inserire una data valida. Operazione annullata");
		}
		if(flash.contains("error")){
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
							
		int deleted = AbsenceManager.removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);
		
		if(deleted > 0){
			flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
		}
		
//		Se si tratta di una modifica, effettuo l'inserimento dopo la rimozione della vecchia assenza
		if(!absenceCode.isEmpty()){
			
			AbsenceType absenceType = AbsenceTypeDao.getAbsenceTypeByCode(absenceCode);
			Verify.verifyNotNull(absenceType, "Codice di assenza %s inesistente!", absenceCode);

			AbsenceInsertReport air = AbsenceManager.insertAbsence(person, dateFrom, Optional.fromNullable(dateTo),
					absenceType,Optional.fromNullable(file), Optional.fromNullable(mealTicket));
//			Verifica errori generali nel periodo specificato
			if(air.hasWarningOrDaysInTrouble()){
				flash.error(String.format(air.getWarnings().iterator().next() + 
						" - %s",air.getDatesInTrouble()));
			}

//			Verifica degli errori sui singoli giorni
			if(air.getTotalAbsenceInsert() == 0 && !air.getAbsences().isEmpty()){

				Multimap<String, LocalDate> errors = ArrayListMultimap.create();

				for(AbsencesResponse ar : air.getAbsences()){
					errors.put(ar.getWarning() + " [codice: " + ar.getAbsenceCode() + "]", ar.getDate());
				}

				flash.error(errors.toString());
			}

//			Verifica per eventuali giorni di reperibilità
			if(air.getAbsenceInReperibilityOrShift() > 0){
				flash.error("Attenzione! verificare le reperibilità nei seguenti giorni : %s", air.datesInReperibilityOrShift());
			}

			if(air.getTotalAbsenceInsert() > 0){
				flash.success("Sostituito codice %s con codice %s in %s assenza/e", 
					absence.absenceType.code,absenceCode,air.getTotalAbsenceInsert());
			}
		}
		
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	}
	
// 	FIXME Questo controller non viene mai usato,Non esiste nemmeno la vista 
//	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
//	public static void insertPersonChildren(){
//		int month = new LocalDate().getMonthOfYear();
//		int year = new LocalDate().getYear();
//		//List<Person> personList = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
//		List<Person> personList = PersonDao.list(Optional.<String>absent(), OfficeDao.getOfficeAllowed(Optional.<User>absent())
//				, false, new LocalDate(year,month,1), new LocalDate(year,month,1).dayOfMonth().withMaximumValue(), true).list();
//		render(personList);
//	}
	
	public static class AttachmentsPerCodeRecap {
		
		List<Absence> absenceSameType = new ArrayList<Absence>();
		
		public String getCode() {
			return absenceSameType.get(0).absenceType.code;
		}
		
	}
	
	public static void manageAttachmentsPerCode(Integer year, Integer month){
		
		LocalDate beginMonth = new LocalDate(year, month, 1);
		
		//Prendere le assenze ordinate per tipo
		List<Absence> absenceList = AbsenceDao.getAbsencesInPeriod(Optional.<Person>absent(), beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), true);
	
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
		notFoundIfNull(absence);
		
		rules.checkIfPermitted(absence.personDay.person.office);

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
		List<Absence> personAbsenceList = AbsenceDao.getAbsencesInPeriod(Optional.fromNullable(person), new LocalDate(year, month,1), Optional.fromNullable(new LocalDate(year, month,1).dayOfMonth().withMaximumValue()), false);

		for(Absence abs : personAbsenceList){
			if (abs.absenceFile.get() != null){
				personAbsenceListWithFile.add(abs);
			}
		}
		render(personAbsenceListWithFile, year, month, person);
	}	
	
	public static void absenceInPeriod(Person person,
			LocalDate from,
			LocalDate to){
    	
//		Se la persona non è specificata si usa l'utente attualmente connesso
		if(!person.isPersistent())
			person = Security.getUser().get().person;
//		Se le date non sono specificate imposto il giorno corrente
		if(from == null || to == null){
			from = LocalDate.now();
			to = LocalDate.now();
		}
				
//      Capita solo se l'utente connesso è l'admin, che però non può accedere a questo controller per via delle drools!
//		if(person == null){
//			flash.error("Persona inesistente");
//			Stampings.personStamping(Security.getUser().get().person.id, new LocalDate().getYear(), new LocalDate().getMonthOfYear());
//		}
				
		List<Absence> missioni = Lists.newArrayList();
		List<Absence> ferie = Lists.newArrayList();
		List<Absence> riposiCompensativi = Lists.newArrayList();
		List<Absence> altreAssenze = Lists.newArrayList();
		
		List<Person> personList = PersonDao.list(Optional.<String>absent(), 
				OfficeDao.getOfficeAllowed(Optional.<User>absent()), false, from, to, true).list();
		
		if(from.isAfter(to)){
			flash.error("Intervallo non valido (%s - %s)", from,to);
			render(personList, person, from, to, missioni, ferie, riposiCompensativi, altreAssenze);
		}
		
		rules.checkIfPermitted(person.office);
		
		List<Absence> absenceList = AbsenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
				Optional.<String>absent(), from, to, Optional.fromNullable(JustifiedTimeAtWork.AllDay), false, false);
		
		for(Absence abs : absenceList){
			if(AbsenceTypeMapping.MISSIONE.is(abs.absenceType)){
				missioni.add(abs);
			}
			else if(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.is(abs.absenceType) ||
					AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.is(abs.absenceType) || 
					AbsenceTypeMapping.FESTIVITA_SOPPRESSE.is(abs.absenceType)){
				ferie.add(abs);
			}
			else if(AbsenceTypeMapping.RIPOSO_COMPENSATIVO.is(abs.absenceType)){
				riposiCompensativi.add(abs);
			}
			else
				altreAssenze.add(abs);
		}
		render(personList, person, absenceList, from, to, missioni, ferie, riposiCompensativi, altreAssenze, person.id);

	}
	
}
