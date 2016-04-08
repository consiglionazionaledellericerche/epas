package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;

import com.mysema.query.SearchResults;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationState;
import com.sun.xml.internal.ws.developer.MemberSubmissionAddressing.Validation;

import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import injection.StaticInject;

import lombok.extern.slf4j.Slf4j;

import manager.PeriodManager;

import models.Institute;
import models.Office;
import models.Person;
import models.Role;
import models.User;

import org.assertj.core.util.Maps;
import org.assertj.core.util.Sets;
import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import synch.perseoconsumers.office.OfficePerseoConsumer;
import synch.perseoconsumers.people.PeoplePerseoConsumer;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class Synchronizations extends Controller {

  @Inject 
  static OfficeDao officeDao;
  @Inject
  static PersonDao personDao;
  @Inject 
  static RoleDao roleDao;
  @Inject
  static OfficePerseoConsumer officePerseoConsumer;
  @Inject
  static PeoplePerseoConsumer peoplePerseoConsumer;
  @Inject
  static PeriodManager periodManager;
  @Inject
  private static WrapperModelFunctionFactory wrapperFunctionFactory;
  
  /**
   * Gli istituti in epas da sincronizzare.
   */
  public static void institutes(String name) {
    
    List<Institute> institutes = officeDao.institutes(Optional.<String>fromNullable(name),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN)).list();
    
    Map<String, Institute> perseoInstitutesByCds = officePerseoConsumer.perseoInstitutesByCds();
    
    Map<String, Office> perseoOfficeByCodeId = Maps.newHashMap();
    for (Institute institute : perseoInstitutesByCds.values()) {
      for (Office office : institute.seats) {
        perseoOfficeByCodeId.put(office.codeId, office);
      }
    }
    
    render(institutes, perseoInstitutesByCds, perseoOfficeByCodeId);
  }
  
  /**
   * Gli istituti non in epas, si possono importare.
   */
  public static void otherInstitutes() {
    
    List<Institute> perseoInstitutes = officePerseoConsumer.perseoInstitutes();
    
    List<Institute> institutes = officeDao.institutes(Optional.<String>absent(),
        Security.getUser().get(), roleDao.getRoleByName(Role.TECNICAL_ADMIN)).list();
    
    Map<Long, Institute> epasInstituteByPerseoId = Maps.newHashMap();
    Map<Long, Office> epasOfficesByPerseoId = Maps.newHashMap();
    for (Institute institute : institutes) {
      if (institute.perseoId != null) {
        epasInstituteByPerseoId.put(institute.perseoId, institute);
      }
      for (Office office : institute.seats) {
        if (office.perseoId != null) {
          epasOfficesByPerseoId.put(office.perseoId, office);
        }
      }
    }
    
    render(perseoInstitutes, epasInstituteByPerseoId, epasOfficesByPerseoId);
  }
  
  /**
   * Lega l'istituto epas al perseoId. Da utilizzare manualmente con cautela!!!
   * @param epasInstituteId
   * @param perseoId
   */
  public static void joinInstitute(Long epasInstituteId, Long perseoId) {
    Optional<Institute> institute = officeDao.instituteById(epasInstituteId);
    Verify.verify(institute.isPresent());
    Verify.verifyNotNull(perseoId);

    Optional<Institute> instituteInPerseo = officePerseoConsumer
        .perseoInstituteByInstitutePerseoId(perseoId);
    Verify.verify(instituteInPerseo.isPresent());
    
    //copy ( TODO: update method)
    institute.get().perseoId = instituteInPerseo.get().perseoId;
    institute.get().cds = instituteInPerseo.get().cds;
    institute.get().code = instituteInPerseo.get().code;
    institute.get().name = instituteInPerseo.get().name;
    institute.get().save();
    
    log.info("Associato istituto={} al perseoId={}", institute.get().toString(), perseoId);
    flash.success("Operazione effettuata correttamente");
    institutes(null);
  }
  
  /**
   * Lega la sede epas al perseoId. Da utilizzare manualmente con cautela!!!
   * @param epasInstituteId
   * @param perseoId
   */
  public static void joinOffice(Long epasOfficeId, Long perseoId) {
    Office office= officeDao.getOfficeById(epasOfficeId);
    Verify.verifyNotNull(office);
    Verify.verifyNotNull(perseoId);
    
    Optional<Institute> instituteWithThatSeat = 
        officePerseoConsumer.perseoInstituteByOfficePerseoId(perseoId);
    Verify.verify(instituteWithThatSeat.isPresent());
    
    Office perseoOffice = instituteWithThatSeat.get().seats.iterator().next();
   
    //copy ( TODO: update method)
    office.perseoId = perseoOffice.perseoId;
    office.code = perseoOffice.code;
    office.name = perseoOffice.name;
    office.address = perseoOffice.address;
    
    office.save();
    
    log.info("Associata sede={} al perseoId={}", office.toString(), perseoId);
    flash.success("Operazione effettuata correttamente");
    institutes(null);
  }
  
  /**
   * Importa la sede da perseo, (e l'istituto se non è già presente).
   */
  public static void importOffice(Long seatPerseoId) {
    
    //Prendere da perseo quella sede.
    Optional<Institute> instituteWithThatSeat = 
        officePerseoConsumer.perseoInstituteByOfficePerseoId(seatPerseoId);
    if (!instituteWithThatSeat.isPresent()) {
      flash.error("Niente da fare :(((.");
      otherInstitutes();
    }
    Office seat = instituteWithThatSeat.get().seats.iterator().next();
    
    // Salvataggio istituto
    Optional<Institute> institute = officeDao.byCds(instituteWithThatSeat.get().cds);
    if (!institute.isPresent()) {
      
      //Istituto non presente
      
      validation.valid(instituteWithThatSeat);
      if (validation.hasErrors()) {
        // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
        log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
            + "nel suo istituto. errors={}.", seatPerseoId, validation.errors());
        flash.error("La sede selezionata non può essere importata a causa di errori.");
        otherInstitutes();
      } 
      
      instituteWithThatSeat.get().save();
      institute = instituteWithThatSeat;
    } else {
      
      //Istituto già presente, aggiungo la nuova sede alla sua lista e sistemo la relazione.
      institute.get().seats.add(seat);
      seat.institute = institute.get();
    }
    
    //Salvataggio sede
    validation.valid(seat);
    if (validation.hasErrors()) {
      // notifica perseo ci ha mandato un oggetto che in epas non può essere accettato!
      log.info("L'importazione della sede con perseoId={} ha comportato errori di validazione "
          + "nella sede. errors={}.", seatPerseoId, validation.errors());
      flash.error("La sede selezionata non può essere importata a causa di errori.");
      otherInstitutes();
    } 
    
    // TODO: spostare in un creator epas che venga utilizzato anche nelle crud (finchè non spariranno).
    seat.beginDate = new LocalDate(LocalDate.now().getYear() - 1, 12, 31);
    periodManager.updatePropertiesInPeriodOwner(seat);
    seat.save();
    
    flash.success("La sede %s è stata importata con successo da Perseo!", seat.toString());
    
    otherInstitutes();
  }
  
  
  /**
   * Le persone in epas da sincronizzare.
   */
  public static void people(Long officeId) {
    
    Office office;
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
      notFoundIfNull(office);
    } else {
      office = officeDao.allOffices().list().get(0);
    }
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    
    List<Person> people = personDao
        .listFetched(Optional.<String>absent(), offices, false, null, null, false)
        .list();
    
    List<IWrapperPerson> wrapperedPeople = FluentIterable.from(people)
        .transform(wrapperFunctionFactory.person()).toList();
    
    Map<Integer, Person> perseoPeopleByNumber = peoplePerseoConsumer.perseoPeopleByNumber();
    
    render(wrapperedPeople, perseoPeopleByNumber);
  }
  
  /**
   * Le persone non in epas si possono importare.
   */
  public static void otherPeople(Long officeId) {
    
    Office office;
    if (officeId != null) {
      office = officeDao.getOfficeById(officeId);
      notFoundIfNull(office);
    } else {
      office = officeDao.allOffices().list().get(0);
    }
    Set<Office> offices = Sets.newHashSet();
    offices.add(office);
    
    
    List<Person> people = personDao
        .listFetched(Optional.<String>absent(), offices, false, null, null, false)
        .list();
    
    Map<Integer, Person> perseoPeopleByNumber = peoplePerseoConsumer.perseoPeopleByNumber();
    
    render(people, perseoPeopleByNumber);
  }
  
  public static void joinPerson(Long epasPersonId, Long perseoId) {
    renderText("ok");
  }
  
  
  
  
}
