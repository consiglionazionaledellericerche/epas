package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.ShiftDao;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftManager;
import models.ShiftType;
import models.User;
import models.dto.ShiftEvent;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author daniele
 * @since 15/05/17.
 */
@With(Resecure.class)
@Slf4j
public class Calendar extends Controller {

  @Inject
  static ShiftManager shiftManager;
  @Inject
  static ShiftDao shiftDao;
  @Inject
  static ObjectMapper mapper;


  public static void show(long activityId) {
    User currentUser = Security.getUser().get();

    List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .collect(Collectors.toList());

    final ShiftType activity = activities.get(0);
    session.put("currentShiftActivity", activity.id);

    render(activities, activity);
  }

  public static void shifts(LocalDate start, LocalDate end) throws JsonProcessingException {

    final ShiftType type = ShiftType.findById(Long.parseLong(session.get("currentShiftActivity")));
    List<ShiftEvent> events = new ArrayList<>();

    type.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift)
        .forEach(personShift -> events.addAll(shiftManager
            .toEvents(
                shiftDao.getPersonShiftDaysByPeriodAndType(start, end, type, personShift.person))));

    renderJSON(mapper.writeValueAsString(events));
  }

  public static void changeShift(long id, LocalDate originalStart, LocalDate originalEnd,
      LocalDate start, LocalDate end) {
    log.debug(
        "CHIAMATA LA MODIFICA DEL TURNO: personId {} - start-orig {} "
            + "- end-orig{} - start {} - end {}", id, originalStart, originalEnd, start, end);
//    response.status = StatusCode.BAD_REQUEST;
  }
}
