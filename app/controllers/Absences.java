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
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.QualificationMapping;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.QualificationDao;


@With( {Resecure.class, RequestInit.class} )
public class Absences extends Controller{

	@Inject
	private static SecurityRules rules;
	@Inject
	private static AbsenceTypeDao absenceTypeDao;
	@Inject
	private static QualificationDao qualificationDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static AbsenceDao absenceDao;
	@Inject
	private static AbsenceManager absenceManager;
	@Inject
	private static OfficeDao officeDao;

	public static void absences(int year, int month) {
		Person person = Security.getUser().get().person;
		YearMonth yearMonth = new YearMonth(year,month);
		Map<AbsenceType,Long> absenceTypeInMonth = 
				absenceTypeDao.getAbsenceTypeInPeriod(person,
						DateUtility.getMonthFirstDay(yearMonth), 
						Optional.fromNullable(DateUtility.getMonthLastDay(yearMonth)));

		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(absenceTypeInMonth, year, month, month_capitalized);
	}

	public static void absenceInMonth(String absenceCode, int year, int month){
		Person person = Security.getUser().get().person;
		YearMonth yearMonth = new YearMonth(year,month);

		List<Absence> absences = absenceDao.getAbsenceByCodeInPeriod(
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

		SimpleResults<AbsenceType> simpleResults = absenceTypeDao.getAbsences(Optional.fromNullable(name));
		List<AbsenceType> absenceList = simpleResults.paginated(page).getResults();

		render(absenceList, name, simpleResults);
	}

	public static void insertAbsenceCode(){

		List<JustifiedTimeAtWork> justifiedTimeAtWorkList = Lists.newArrayList(JustifiedTimeAtWork.values());
		List<AccumulationType> accumulationTypeList = Lists.newArrayList(AccumulationType.values());
		List<AccumulationBehaviour> accumulationBehaviourList = Lists.newArrayList(AccumulationBehaviour.values());

		render(justifiedTimeAtWorkList, accumulationTypeList, accumulationBehaviourList);
	}

	public static void saveAbsenceCode(
			@Required @Valid AbsenceType absenceType,
			@Required String justifiedTimeAtWork,
			boolean tecnologi,
			boolean tecnici,
			AbsenceTypeGroup absenceTypeGroup,
			String accBehaviour,
			String accType,
			String codiceSostituzione
			){

		if (!(tecnologi || tecnici)){
			params.flash();
			flash.error("Selezionare almeno una categoria tra Tecnologi e Tecnici");
			insertAbsenceCode();
		}

		if (validation.hasErrors()){
			flash.error(validation.errorsMap().toString());
//			TODO modificare il template per visualizzare gli errori di validazione
			insertAbsenceCode();
		}

		absenceType.justifiedTimeAtWork = JustifiedTimeAtWork.getByDescription(justifiedTimeAtWork);

		Range<Integer> qualifiche;
		if(tecnologi && tecnici){
			qualifiche = QualificationMapping.TECNICI.getRange().span(QualificationMapping.TECNOLOGI.getRange());
		}
		else if(tecnologi){
			qualifiche = QualificationMapping.TECNOLOGI.getRange();
		}
		else{
			qualifiche = QualificationMapping.TECNICI.getRange();
		}

		for(int i = qualifiche.lowerEndpoint(); i <= qualifiche.upperEndpoint(); i++){
			Qualification q = qualificationDao.byQualification(i).orNull();
			absenceType.qualifications.add(q);
		}

		if(absenceTypeGroup.label != null && !absenceTypeGroup.label.isEmpty()){
			absenceTypeGroup.accumulationBehaviour = AccumulationBehaviour.getByDescription(accBehaviour);
			absenceTypeGroup.accumulationType = AccumulationType.getByDescription(accType);

			if(accBehaviour.equals(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation.description)){
				absenceTypeGroup.replacingAbsenceType = absenceTypeDao.getAbsenceTypeByCode(codiceSostituzione).orNull();
			}
			absenceType.absenceTypeGroup = absenceTypeGroup;
			absenceTypeGroup.save();
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

		Person person = personDao.getPersonById(personId);
		Preconditions.checkNotNull(person);

		rules.checkIfPermitted(person.office);

		List<AbsenceType> frequentAbsenceTypeList = absenceTypeDao.getFrequentTypes();
		List<AbsenceType> allCodes = absenceTypeDao.getAbsenceTypeFromEffectiveDate(date);

		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes);
	}

	public static void insert(@Required Person person, 
			@NotNull LocalDate dateFrom,
			LocalDate dateTo,
			@Required String absenceCode, 
			Blob file){

		Optional<AbsenceType> absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);

		if(!absenceType.isPresent()){
			flash.error("Codice di assenza %s inesistente!", absenceCode);
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());	
		}

		if(!person.isPersistent()){
			flash.error("Persona specificata inesistente!");
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());	
		}

		rules.checkIfPermitted(person.office);

		AbsenceInsertReport air = absenceManager.insertAbsence(person, dateFrom,Optional.fromNullable(dateTo), 
				absenceType.get(), Optional.fromNullable(file), Optional.<String>absent(), false);

		//Verifica errori generali nel periodo specificato
		if(air.hasWarningOrDaysInTrouble()){

			flash.error(String.format(air.getWarnings().iterator().next() + 
					" - %s",air.getDatesInTrouble()));
		}

		//Verifica degli errori sui singoli giorni
		if(air.getTotalAbsenceInsert() == 0 && !air.getAbsences().isEmpty()){

			Multimap<String, LocalDate> errors = ArrayListMultimap.create();

			for(AbsencesResponse ar : air.getAbsences()){
				errors.put(ar.getWarning() + " [codice: " + ar.getAbsenceCode() + "]", ar.getDate());
			}
			flash.error(errors.toString());
		}
		
		//Verifica per eventuali giorni di reperibilità
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
		//		FIXME decidere se permetterlo all'admin, ed eventualmente sistemare il controllo, o il permesso
		rules.checkIfPermitted(Security.getUser().get().person.office);

		AbsenceType abt = absenceTypeDao.getAbsenceTypeById(absenceCodeId);

		List<JustifiedTimeAtWork> justifiedTimeAtWorkList = Lists.newArrayList(JustifiedTimeAtWork.values());
		List<AccumulationType> accumulationTypeList = Lists.newArrayList(AccumulationType.values());
		List<AccumulationBehaviour> accumulationBehaviourList = Lists.newArrayList(AccumulationBehaviour.values());

		boolean tecnologo = false;
		boolean tecnico = false;

		for(Qualification q : abt.qualifications){
			tecnologo = !tecnologo ? QualificationMapping.TECNOLOGI.contains(q) : tecnologo;
			tecnico = !tecnico ? QualificationMapping.TECNICI.contains(q) : tecnico;
		}

		render(abt,justifiedTimeAtWorkList, accumulationTypeList, accumulationBehaviourList,tecnologo,tecnico);
	}


	public static void updateCode(@Required @Valid AbsenceType absenceType,
			boolean tecnologi,
			boolean tecnici){

		rules.checkIfPermitted(Security.getUser().get().person.office);

		if (!(tecnologi || tecnici)){
			params.flash();
			flash.error("Selezionare almeno una categoria tra Tecnologi e Tecnici");
			insertAbsenceCode();
		}

		Verify.verify(absenceType.isPersistent(),"Codice d'assenza inesistente!");

		absenceType.qualifications.clear();
		Range<Integer> qualifiche;
		if(tecnologi && tecnici){
			qualifiche = QualificationMapping.TECNICI.getRange().span(QualificationMapping.TECNOLOGI.getRange());
		}
		else if(tecnologi){
			qualifiche = QualificationMapping.TECNOLOGI.getRange();
		}
		else{
			qualifiche = QualificationMapping.TECNICI.getRange();
		}

		for(int i = qualifiche.lowerEndpoint(); i <= qualifiche.upperEndpoint(); i++){
			Qualification q = qualificationDao.byQualification(i).orNull();
			absenceType.qualifications.add(q);
		}

		absenceType.save();

		Logger.info("Modificato codice di assenza %s", absenceType.code);

		flash.success("Modificato codice di assenza %s", absenceType.code);
		Absences.manageAbsenceCode(null, null);
	}

	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = absenceDao.getAbsenceById(absenceId);

		Verify.verify(absence != null,"Assenza specificata inesistente!");

		rules.checkIfPermitted(absence.personDay.person.office);

		List<AbsenceType> frequentAbsenceTypeList = absenceTypeDao.getFrequentTypes();
		List<AbsenceType> allCodes = absenceTypeDao.getAbsenceTypeFromEffectiveDate(absence.personDay.date);

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

		//Logica
		int deleted = absenceManager.removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);

		if(deleted > 0){
			flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
		}

		//Se si tratta di una modifica, effettuo l'inserimento dopo la rimozione della vecchia assenza
		if(!absenceCode.isEmpty()){

			Optional<AbsenceType> absenceType = absenceTypeDao.getAbsenceTypeByCode(absenceCode);

			if(!absenceType.isPresent()) {
				flash.error("Codice di assenza %s inesistente!", absenceCode);
				Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
			}

			AbsenceInsertReport air = absenceManager.insertAbsence(person, dateFrom, Optional.fromNullable(dateTo),
					absenceType.get(),Optional.fromNullable(file), Optional.fromNullable(mealTicket), false);

			//Verifica errori generali nel periodo specificato
			if(air.hasWarningOrDaysInTrouble()){
				flash.error(String.format(air.getWarnings().iterator().next() + 
						" - %s",air.getDatesInTrouble()));
			}

			//Verifica degli errori sui singoli giorni
			if(air.getTotalAbsenceInsert() == 0 && !air.getAbsences().isEmpty()){

				Multimap<String, LocalDate> errors = ArrayListMultimap.create();

				for(AbsencesResponse ar : air.getAbsences()){
					errors.put(ar.getWarning() + " [codice: " + ar.getAbsenceCode() + "]", ar.getDate());
				}

				flash.error(errors.toString());
			}

			//Verifica per eventuali giorni di reperibilità
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

	public static class AttachmentsPerCodeRecap {

		List<Absence> absenceSameType = new ArrayList<Absence>();

		public String getCode() {
			return absenceSameType.get(0).absenceType.code;
		}

	}

	public static void manageAttachmentsPerCode(Integer year, Integer month){

		LocalDate beginMonth = new LocalDate(year, month, 1);

		//Prendere le assenze ordinate per tipo
		List<Absence> absenceList = absenceDao.getAbsencesInPeriod(
				Optional.<Person>absent(), beginMonth, 
				Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), true);

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
		Absence absence = absenceDao.getAbsenceById(id); 
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


		List<Absence> absList = absenceDao.getAbsenceByCodeInPeriod(
				Optional.<Person>absent(),Optional.fromNullable(code), 
				new LocalDate(year, month, 1), 
				new LocalDate(year, month, 1).dayOfMonth().withMaximumValue(), 
				Optional.<JustifiedTimeAtWork>absent(), true, false);
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
		Person person = personDao.getPersonById(personId);
		//Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<Absence> personAbsenceListWithFile = new ArrayList<Absence>();
		List<Absence> personAbsenceList = absenceDao.getAbsencesInPeriod(Optional.fromNullable(person), new LocalDate(year, month,1), Optional.fromNullable(new LocalDate(year, month,1).dayOfMonth().withMaximumValue()), false);

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

		List<Absence> missioni = Lists.newArrayList();
		List<Absence> ferie = Lists.newArrayList();
		List<Absence> riposiCompensativi = Lists.newArrayList();
		List<Absence> altreAssenze = Lists.newArrayList();

		List<Person> personList = personDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, from, to, true).list();

		if(from.isAfter(to)){
			flash.error("Intervallo non valido (%s - %s)", from,to);
			render(personList, person, from, to, missioni, ferie, riposiCompensativi, altreAssenze);
		}

		rules.checkIfPermitted(person.office);

		List<Absence> absenceList = absenceDao.getAbsenceByCodeInPeriod(Optional.fromNullable(person), 
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
