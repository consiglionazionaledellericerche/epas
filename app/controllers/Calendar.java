package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.AbsenceDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.ShiftManager2;
import models.Person;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftType;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.ShiftEvent;
import models.enumerate.ShiftSlot;
import models.enumerate.Troubles;

import org.jcolorbrewer.ColorBrewer;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * @author arianna
 * @author daniele
 * @since 15/05/17.
 */
@With(Resecure.class)
@Slf4j
public class Calendar extends Controller {

  @Inject
  static ShiftDao shiftDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonShiftDayDao personShiftDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static ShiftManager2 shiftManager2;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  static AbsenceDao absenceDao;

  public static void show(ShiftType activity) {
    User currentUser = Security.getUser().get();

    List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);
    
    List<Person> people = activitySelected.personShiftShiftTypes.stream()
        .filter(personShiftShiftType -> personShiftShiftType.dateRange().contains(LocalDate.now()))
        .map(personShiftShiftType -> personShiftShiftType.personShift.person )
        .collect(Collectors.toList());

    render(activities, activitySelected, people);
  }


  /*
   * Calvella un turno dal Database
   */
  public static void deleteShift(PersonShiftDay psd) {
    log.debug("lol");
    notFoundIfNull(psd);
    response.status = Http.StatusCode.BAD_REQUEST;
    renderText("Un messaggio qualsiasi");
    //psd.delete();
    
  }

  
  /*
   * Carica i turni dal database per essere visualizzati nel calendario
   */
  public static void events(ShiftType shiftType, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    List<ShiftEvent> events = new ArrayList<>();

    List<Person> people = shiftType.personShiftShiftTypes.stream()
        .filter(personShiftShiftType -> personShiftShiftType.dateRange().contains(start))
        .map(personShiftShiftType -> personShiftShiftType.personShift.person )
        .collect(Collectors.toList());

    int index = 0;

    ColorBrewer sequentialPalettes = ColorBrewer.valueOf("YlOrBr");
    Color[] myGradient = sequentialPalettes.getColorPalette(11);
    
    // prende i turni associati alle persone attive in quel turno
    for (Person person : people) {
      final String color = "#" + Integer.toHexString(myGradient[index].getRGB() & 0xffffff);
      events.addAll(shiftEvents(shiftType, person, start, end, color));
      events.addAll(absenceEvents(person, start, end));
      index++;
    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }


  /*
   * Carica la lista dei turni di un certo tipo associati ad una determinata persona in
   * un intervallo di tempo
   * 
   * @param ShiftType shiftType: tipo di turno
   * @param Person person: persona associata ai turni
   * @param LocalDate start: data inizio intervallo di tempo
   * @param LocalDate end: data fine intervallo di tempo
   * 
   * @output List<ShiftEvent>: lista di eventi 
   */
  private static List<ShiftEvent> shiftEvents(ShiftType shiftType, Person person, LocalDate start,
      LocalDate end, String color) {

    return shiftDao.getPersonShiftDaysByPeriodAndType(start, end, shiftType, person).stream()
        .map(shiftDay -> {

          return ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(shiftDay.shiftSlot)
              .personShiftDayId(shiftDay.id)
              .title(shiftDay.getSlotTime() + '\n' + person.fullName())
              .start(shiftDay.date)
              .color(color)
              .className("removable")
              .textColor("black")
              .borderColor("black")
              .build();

        }).collect(Collectors.toList());
  }
  
  private static List<ShiftEvent> absenceEvents(Person person, LocalDate start, LocalDate end) {

    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.all_day, JustifiedTypeName.assign_all_day);

    List<Absence> absences = absenceDao
        .findByPersonAndDate(person, start, Optional.fromNullable(end), Optional.absent()).list()
        .stream()
        .filter(absence -> types.contains(absence.justifiedType.name))
        .sorted(Comparator.comparing(ab -> ab.personDay.date)).collect(Collectors.toList());
    List<ShiftEvent> events = new ArrayList<>();
    ShiftEvent event = null;

    for (Absence abs : absences) {

      /**
       * Per quanto riguarda gli eventi 'allDay':
       *
       * La convensione del fullcalendar è quella di avere il parametro end = null
       * nel caso di eventi su un singolo giorno, mentre nel caso di evento su più giorni il
       * parametro end assume il valore del giorno successivo alla fine effettiva
       * (perchè ne imposta l'orario alla mezzanotte).
       */
      if (event == null
          || event.getEnd() == null && !event.getStart().plusDays(1).equals(abs.personDay.date)
          || event.getEnd() != null && !event.getEnd().equals(abs.personDay.date)) {
        event = ShiftEvent.builder()
            .allDay(true)
            .title("Assenza di " + abs.personDay.person.fullName())
            .start(abs.personDay.date)
            .durationEditable(false)
            .editable(false)
            .startEditable(false)
            //.color("#"+Integer.toHexString(myGradient[index].getRGB() & 0xffffff))
            .build();

        events.add(event);
      } else {
        event.setEnd(abs.personDay.date.plusDays(1));
      }

    }

    return events;
  }



  /*
   * Chiamata dal fullCalendar dei turni per ogni evento di drop di un turno sul calendario.
   * Controlla se il turno passato come parametro può essere salvato in un dato giorno 
   * ed eventualmente lo salva.
   * 
   * @param long personShiftDayId: id del persnShiftDay da controllare
   * @param LocalDate newDate: giorno nel quale salvare il turno
   * 
   * @out String messages: eventuali messaggi di errore
   */
  public static void changeShift(long personShiftDayId, LocalDate newDate) {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    log.debug("Chiamato metodo changeShift: personShiftDayId {} - newDate {} ", personShiftDayId, newDate);
   
    List<String> errors = new ArrayList<>();
    String messages = "";
 
    // legge il turno da spostare
    PersonShiftDay oldShift = shiftDao.getPersonShiftDayById(personShiftDayId);

    // controlla se può essere spostato nella nuova data
    errors = shiftManager2.checkShiftDay(oldShift, newDate);
    if (errors.isEmpty()) {
      //salva il nuovo turno
      PersonShiftDay newShift = oldShift;
      newShift.date = newDate;
      newShift.save();
      log.info("Aggiornato PersonShiftDay = {} con {}\n",
          oldShift, newShift);
      
      // cancella quello vecchio
      shiftDao.deletePersonShiftDay(oldShift);

    } else {      
      // restituisce il messaggi per gli errori
      for (String errCode: errors) {
        String msg = "calendar.".concat(errCode);
        messages.concat(Messages.get(msg)).concat("<br />");
      }
      
      response.status = Http.StatusCode.BAD_REQUEST;
      renderText(messages);
    }

  }


  
  /*
   * Cancella un personShiftDays dal DB
   */
  private static void cancShifts(PersonShiftDay personShiftDay) {  
      shiftDao.deletePersonShiftDay(personShiftDay);
      log.info("Cancellato PersonShiftDay = {} con {}\n",
          personShiftDay, personShiftDay.personShift.person);
  }
  

  public static void newShift(long personId, LocalDate date, ShiftSlot shiftSlot,
      ShiftType shiftType) {
    log.debug("CHIAMATA LA CREAZIONE DEL TURNO: personId {} - day {} - slot {} - shiftType {}",
        personId, date, shiftSlot, shiftType);
    // TODO: creare il personShiftDay se rispetta tutti i canoni e le condizioni di possibile esistenza
    // TODO: ricordarsi di controllare se la persona è attiva sull'attività al momento della creazione del 
    // personshiftDay
    // TODO: vediamo se la renderJSON è il metodo migliore per ritornare l'id del personShiftDay creato
    
    String color = ""; //TODO:
    
    // crea il personShiftDay
    PersonShiftDay personShiftDay = new PersonShiftDay();
    personShiftDay.date = date;
    personShiftDay.shiftType = shiftType;
    personShiftDay.setShiftSlot(shiftSlot);
    personShiftDay.personShift = shiftDao.getPersonShiftByPersonAndType(personId, shiftType.type);
    
    // controlla che possa essere salvato nel giorno
    List<String> errors = shiftManager2.checkShiftDay(personShiftDay, date);
    

    // contruisce l'evento
    //TODO: con gli errori? e poi li prendi nel calendario? (messaggi o errCode?) Oppure?
    ShiftEvent event = ShiftEvent.builder()
        .allDay(true)
        .shiftSlot(personShiftDay.shiftSlot)
        .personShiftDayId(personShiftDay.id)
        .title(personShiftDay.getSlotTime() + '\n' + personShiftDay.personShift.person.fullName())
        .start(personShiftDay.date)
        .color(color)
        // TODO: .error()
        .className("removable")
        .textColor("black")
        .borderColor("black")
        .build();
    
     //renderJSON(mapper.writeValueAsString(event));
  }


}
