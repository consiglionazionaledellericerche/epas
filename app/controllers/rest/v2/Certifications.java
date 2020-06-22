package controllers.rest.v2;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.text.DateFormatter;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import cnr.sync.dto.v2.CertificationAbsenceDto;
import cnr.sync.dto.v2.CertificationCompetencesDto;
import cnr.sync.dto.v2.CertificationDto;
import cnr.sync.dto.v2.CertificationMealTicketDto;
import cnr.sync.dto.v2.CertificationTrainingHoursDto;
import controllers.Resecure;
import controllers.Resecure.BasicAuth;
import controllers.Resecure.NoCheck;
import dao.AbsenceDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.WorkingTimeTypeDao;
import helpers.JsonResponse;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.dto.show.SeatCertification.PersonCertification;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.PersonCertData;
import manager.attestati.service.PersonMonthlySituationData;
import models.Certification;
import models.Office;
import models.Person;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Certifications extends Controller{

  @Inject
  static PersonMonthlySituationData monthData;
  @Inject
  static PersonDao personDao;
  @Inject
  static SecurityRules rules;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static GsonBuilder gsonBuilder;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static WorkingTimeTypeDao workingTimeTypeDao;
  
  /**
   * Metodo rest che permette di ritornare una lista contenente le informazioni mensili
   * del dipendente (assenze, competenze, ore di formazione, buoni pasto).
   * @param email l'indirizzo mail della persona
   * @param eppn il campo eppn della persona
   * @param personPersoId l'identificativo anagrafico della persona
   * @param year l'anno
   * @param month il mese
   */
  @BasicAuth
  public static void getMonthSituation(String email, String eppn, 
      Long personPersoId, int year, int month) {

    log.debug("Richieste informazioni mensili da applicazione esterna");
    Optional<Person> person = personDao.byEppnOrEmailOrPerseoId(eppn, email, personPersoId);

    if (!person.isPresent()) {
      log.info("Non trovata la persona in base ai parametri passati: "
          + "email = {}, eppn = {}, personPersoId = {}", email, eppn, personPersoId);
      
      JsonResponse.notFound("Indirizzo email incorretto. Non è presente in ePAS la "
          + "mail che serve per la ricerca.");
    }

    rules.checkIfPermitted(person.get().office);
    
    Map<String, Certification> map = monthData.getCertification(person.get(), year, month);
    CertificationDto dto = generateCertDto(map, year, month, person.get());
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(dto));

  }
  
  /**
   * Metodo che ritorna la lista degli oggetti contenenti le info mensili per la generazione
   * delle buste paga verso sistemi esterni per tutti i dipendenti della sede identificata
   * da sedeId nell'anno year e nel mese month.
   * @param sedeId l'identificativo della sede
   * @param year l'anno
   * @param month il mese
   */
  public static void getMonthSituationByOffice(String sedeId, int year, int month) {
    log.debug("Richieste informazioni mensili da applicazione esterna");
    Optional<Office> office = officeDao.byCodeId(sedeId);
    if (!office.isPresent()) {
      notFound();
    }
    rules.checkIfPermitted(office.get()); 
    Set<Office> offices = Sets.newHashSet();
    offices.add(office.get());
    LocalDate start = new LocalDate(year, month, 1);
    LocalDate end = start.dayOfMonth().withMaximumValue();
    List<CertificationDto> list = Lists.newArrayList();
    List<Person> personList = personDao
        .listFetched(Optional.<String>absent(), offices, false, start, end, true).list();
    for (Person person : personList) {
      Map<String, Certification> map = monthData.getCertification(person, year, month);
      CertificationDto dto = generateCertDto(map, year, month, person);
      list.add(dto);
    }
    val gson = gsonBuilder.create();
    renderJSON(gson.toJson(list));
  }

  /**
   * Metodo privato che permette la generazione di un dto contenente informazioni
   * mensili del dipendente.
   * @param map la mappa contenente le informazioni mensili da rielaborare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @param person la persona per cui cercare le informazioni
   * @return il dto contenente le informazioni da inviare al chiamante del servizio rest.
   */   
  private static CertificationDto generateCertDto(Map<String, Certification> map, 
      int year, int month, Person person) {        
    
    List<CertificationCompetencesDto> competences = Lists.newArrayList();
    List<CertificationMealTicketDto> mealTickets = Lists.newArrayList();
    List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();

    LocalDate from;
    LocalDate to;
    String[] places;
    List<CertificationAbsenceDto> absences = searchAbsences(person, year, month);

    for (Map.Entry<String, Certification> entry : map.entrySet()) {
      switch (entry.getValue().certificationType) {
        case ABSENCE:
          break;
        case COMPETENCE:
          places = entry.getValue().content.split(";");
          CertificationCompetencesDto competence = CertificationCompetencesDto.builder()
              .code(places[0])
              .quantity(Integer.parseInt(places[1]))
              .build();
          competences.add(competence);
          break;
        case MEAL:
          CertificationMealTicketDto meal = CertificationMealTicketDto.builder()
              .quantity(Integer.parseInt(entry.getValue().content))
              .build();
          mealTickets.add(meal);
          break;
        case FORMATION:
          places = entry.getValue().content.split(";");
          from = new LocalDate(year, month, Integer.parseInt(places[0]));
          to = new LocalDate(year, month, Integer.parseInt(places[1]));
          CertificationTrainingHoursDto trainingHour = CertificationTrainingHoursDto.builder()
              .from(from)
              .to(to)
              .quantity(Integer.parseInt(places[2]))
              .build();
          trainingHours.add(trainingHour);
          break;
        default:
          break;
      }
    }
    CertificationDto obj = CertificationDto.builder()
        .fullName(person.getFullname())
        .number(person.number)
        .year(year)
        .month(month)
        .absences(absences)
        .competences(competences)
        .mealTickets(mealTickets)
        .trainingHours(trainingHours)
        .build();
    return obj;
  }
  
  /**
   * Metodo privato per la ricerca delle assenze.
   * @param person la persona di cui cercare le assenze
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la lista di dto contenente la informazioni sulle assenze nell'anno/mese fatte 
   *    dalla persona.
   */
  private static List<CertificationAbsenceDto> searchAbsences(Person person, int year, int month) {
    List<CertificationAbsenceDto> absences = Lists.newArrayList();
    LocalDate begin = new LocalDate(year, month, 1);
    LocalDate end = begin.dayOfMonth().withMaximumValue();

    List<Absence> absencesPlus = absenceDao
        .getAbsenceWithNoHInMonth(person, begin, end);
    LocalDate previousDate = null;
    String previousAbsenceCode = null;
    Integer dayBegin = null;
    Integer dayEnd = null;
    Integer timeToJustify = null;
    String justifiedType = "";
    for (Absence abs : absencesPlus) {
      
      String absenceCodeToSend = abs.absenceType.code.toUpperCase();      
      if (previousDate != null && previousDate.plusDays(1).equals(abs.personDay.date)
          && previousAbsenceCode.equals(absenceCodeToSend)) {
        dayEnd = abs.personDay.date.getDayOfMonth();
        previousDate = abs.personDay.date;        
        continue;
      }
      // 2) Fine Assenza più giorni
      if (previousDate != null) {
        
        CertificationAbsenceDto absence = CertificationAbsenceDto.builder()
            .code(previousAbsenceCode)
            .justifiedTime(timeToJustify)
            .justifiedType(justifiedType)
            .from(new LocalDate(year, month, dayBegin))
            .to(new LocalDate(year, month, dayEnd))
            .build();
        absences.add(absence);        
        previousDate = null;
      }

      // 3) Nuova Assenza  
      dayBegin = abs.personDay.date.getDayOfMonth();
      dayEnd = abs.personDay.date.getDayOfMonth();
      previousDate = abs.personDay.date;
      previousAbsenceCode = absenceCodeToSend;
      timeToJustify = abs.justifiedMinutes;
      if (abs.getJustifiedType().name.equals(JustifiedTypeName.all_day) 
          || abs.getJustifiedType().name.equals(JustifiedTypeName.assign_all_day)) {
        Optional<WorkingTimeTypeDay> workingTimeTypeDay = workingTimeTypeDao
            .getWorkingTimeTypeDay(abs.personDay.date, person);
        timeToJustify = workingTimeTypeDay.get().workingTime;
      }
      if (abs.getJustifiedType().name.equals(JustifiedTypeName.complete_day_and_add_overtime)) {
        Optional<WorkingTimeTypeDay> workingTimeTypeDay = workingTimeTypeDao
            .getWorkingTimeTypeDay(abs.personDay.date, person);
        timeToJustify = workingTimeTypeDay.get().workingTime - abs.personDay.getStampingsTime();
      }
      if (abs.getJustifiedType().name.equals(JustifiedTypeName.absence_type_minutes)) {
        timeToJustify = abs.absenceType.justifiedTime;
      }
      justifiedType = abs.getJustifiedType().getLabel();
      
    }
    if (!absencesPlus.isEmpty()) {
      CertificationAbsenceDto absence = CertificationAbsenceDto.builder()
          .code(previousAbsenceCode)
          .justifiedTime(timeToJustify)
          .justifiedType(justifiedType)
          .from(new LocalDate(year, month, dayBegin))
          .to(new LocalDate(year, month, dayEnd))
          .build();
      absences.add(absence);   
    }
      
    
    return absences;
  }
}

