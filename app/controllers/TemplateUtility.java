package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.CompetenceManager;
import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.SecureManager;
import models.AbsenceType;
import models.Institute;
import models.Office;
import models.Person;
import models.Qualification;
import models.Role;
import models.StampType;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.AbsenceTypeMapping;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.AbsenceTypeDao;
import dao.BadgeReaderDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.QualificationDao;
import dao.RoleDao;
import dao.StampingDao;
import dao.UsersRolesOfficesDao;
import dao.wrapper.IWrapperFactory;

/**
 * Metodi usabili nel template.
 * @author alessandro
 *
 */
public class TemplateUtility {
	
	private final SecureManager secureManager;
	private final OfficeDao officeDao;
	private final PersonDao personDao;
	private final QualificationDao qualificationDao;
	private final AbsenceTypeDao absenceTypeDao;
	private final StampingDao stampingDao;
	private final RoleDao roleDao;
	private final BadgeReaderDao badgeReaderDao;

	@Inject
	public TemplateUtility(SecureManager secureManager,
			OfficeDao officeDao, PersonDao personDao,
			QualificationDao qualificationDao, AbsenceTypeDao absenceTypeDao,
			StampingDao stampingDao, RoleDao roleDao, BadgeReaderDao badgeReaderDao) {
		
				this.secureManager = secureManager;
				this.officeDao = officeDao;
				this.personDao = personDao;
				this.qualificationDao = qualificationDao;
				this.absenceTypeDao = absenceTypeDao;
				this.stampingDao = stampingDao;
				this.roleDao = roleDao;
				this.badgeReaderDao = badgeReaderDao;
	}

	//Convertitori mese

	public String monthName(String month) {

		return DateUtility.getName(Integer.parseInt(month));
	}

	public String monthName(Integer month) {

		return DateUtility.getName(month);
	}

	public String monthNameByString(String month){
		if(month != null)
			return DateUtility.getName(Integer.parseInt(month));
		else
			return null;
	}

	public boolean checkTemplate(String profile) {

		return false;
	}


	//Navigazione menu (next/previous month)

	public int computeNextMonth(int month){
		if(month==12)
			return 1;

		return month + 1;
	}

	public int computeNextYear(int month, int year){
		if(month==12)
			return year + 1;

		return year;
	}

	public int computePreviousMonth(int month){
		if(month==1)
			return 12;

		return month - 1;
	}

	public int computePreviousYear(int month, int year){
		if(month==1)
			return year - 1;

		return year;
	}

	//Liste di utilità per i template

	public Set<Office> officesAllowed(){ 
		return secureManager.officesWriteAllowed(Security.getUser().get());
	}

	public List<Qualification> getAllQualifications() {
		return qualificationDao.findAll();
	}

	public List<AbsenceType> getCertificateAbsenceTypes() {
		return absenceTypeDao.certificateTypes();
	}
	
	public List<StampType> getAllStampTypes(){
		return stampingDao.findAll();
	}
	
	public ImmutableList<String> getAllDays() {
		return ImmutableList.of(
				  "lunedì", "martedì", "mercoledì", "giovedì", 
				  "venerdì", "sabato", "domenica");
	}
	
	/**
	 * Gli user associati a tutte le persone appartenenti all'istituto.
	 * @param institute
	 * @return
	 */
	public List<User> usersInInstitute(Institute institute) {
		
		Set<Office> offices = Sets.newHashSet();
		offices.addAll(institute.seats);
		
		List<Person> personList = personDao.listPerseo(Optional.<String>absent(), 
				offices, false, LocalDate.now(), LocalDate.now(), true).list();

		List<User> users = Lists.newArrayList();
		for(Person person : personList) {
			users.add(person.user);
		}
		
		return users;
	}
	
	public List<Role> rolesAssignable(Office office) {
		
		List roles = Lists.newArrayList();

		// TODO: i ruoli impostabili sull'office dipendono da chi esegue la richiesta...
		// e vanno spostati nel secureManager.
		Optional<User> user = Security.getUser();
		if(user.isPresent()) {
			roles.add(roleDao.getRoleByName(Role.TECNICAL_ADMIN));
			roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN));
			roles.add(roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI));
			return roles;
		}
		return roles;
	}
	
	/**
	 * Gli uffici che l'user può assegnare come owner ai BadgeReader.
	 * Il super admin può assegnarlo ad ogni ufficio.
	 * @return
	 */
	public List<Office> officeForBadgeReaders() {
		
		List<Office> offices = Lists.newArrayList();
		
		Optional<User> user = Security.getUser();
		
		//se admin tutti, altrimenti gli office di cui si ha tecnicalAdmin
		// TODO: spostare nel sucureManager
		if(!user.isPresent()) {
			return offices;
		}

		if(user.get().isSuperAdmin()) {
			return officeDao.getAllOffices();
		}
		
		for(UsersRolesOffices uro : user.get().usersRolesOffices) {
			if(uro.role.name.equals(Role.TECNICAL_ADMIN)) {
				offices.add(uro.office);
			}
		}
		return offices;
	}
	
	/**
	 * Gli account di tutti i badgeReader non ancora assegnati ad office.
	 * @param office
	 * @return
	 */
	public List<User> badgeReaderUserForOffice(Office office) {
		
		List<User> users = Lists.newArrayList();
		
		List<User> badgeReaders = badgeReaderDao.usersBadgeReader();
		for (User user : badgeReaders) {
			boolean insert = true;
			for (UsersRolesOffices uro : user.usersRolesOffices) {
				if (uro.office.id.equals(office.id)) {
					insert = false;
					break;
				}
			}
			if (insert) {
				users.add(user);
			}
		}
		return users;
	}
	
	/**
	 * I codici di assenza ordinati dai più utilizzati.
	 * @return
	 */
	public List<AbsenceType> frequentAbsenceTypeList() {

		Optional<AbsenceType> ferCode = absenceTypeDao.getAbsenceTypeByCode(
				AbsenceTypeMapping.FERIE_FESTIVITA_SOPPRESSE_EPAS.getCode());
		Preconditions.checkState(ferCode.isPresent());
		
		return FluentIterable.from(Lists.newArrayList(ferCode.get()))
				.append(absenceTypeDao.getFrequentTypes()).toList();
	}
	
	/**
	 * I codici di assenza attivi ordinati per codice. 
	 * 
	 * @param date
	 * @return
	 */
	public List<AbsenceType> allAbsenceCodes(LocalDate date) {
		return absenceTypeDao.getAbsenceTypeFromEffectiveDate(date);
	}
	
}