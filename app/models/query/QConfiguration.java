package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Configuration;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QConfiguration is a Querydsl query type for Configuration
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QConfiguration extends EntityPathBase<Configuration> {

    private static final long serialVersionUID = 680577106L;

    public static final QConfiguration configuration = new QConfiguration("configuration");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final BooleanPath addWorkingTimeInExcess = createBoolean("addWorkingTimeInExcess");

    public final DateTimePath<java.util.Date> beginDate = createDateTime("beginDate", java.util.Date.class);

    public final BooleanPath calculateIntervalTimeWithoutReturnFromIntevalTime = createBoolean("calculateIntervalTimeWithoutReturnFromIntevalTime");

    public final BooleanPath canInsertMoreAbsenceCodeInDay = createBoolean("canInsertMoreAbsenceCodeInDay");

    public final BooleanPath canPeopleAutoDeclareAbsences = createBoolean("canPeopleAutoDeclareAbsences");

    public final BooleanPath canPeopleAutoDeclareWorkingTime = createBoolean("canPeopleAutoDeclareWorkingTime");

    public final BooleanPath canPeopleUseWebStamping = createBoolean("canPeopleUseWebStamping");

    public final EnumPath<models.enumerate.CapacityCompensatoryRestFourEight> capacityFourEight = createEnum("capacityFourEight", models.enumerate.CapacityCompensatoryRestFourEight.class);

    public final EnumPath<models.enumerate.CapacityCompensatoryRestOneThree> capacityOneThree = createEnum("capacityOneThree", models.enumerate.CapacityCompensatoryRestOneThree.class);

    public final NumberPath<Integer> dayExpiryVacationPastYear = createNumber("dayExpiryVacationPastYear", Integer.class);

    public final NumberPath<Integer> dayOfPatron = createNumber("dayOfPatron", Integer.class);

    public final StringPath emailToContact = createString("emailToContact");

    public final DateTimePath<java.util.Date> endDate = createDateTime("endDate", java.util.Date.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath holydaysAndVacationsOverPermitted = createBoolean("holydaysAndVacationsOverPermitted");

    public final NumberPath<Integer> hourMaxToCalculateWorkTime = createNumber("hourMaxToCalculateWorkTime", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath ignoreWorkingTimeWithAbsenceCode = createBoolean("ignoreWorkingTimeWithAbsenceCode");

    public final DateTimePath<java.util.Date> initUseProgram = createDateTime("initUseProgram", java.util.Date.class);

    public final BooleanPath insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime = createBoolean("insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime");

    public final StringPath instituteName = createString("instituteName");

    public final BooleanPath inUse = createBoolean("inUse");

    public final BooleanPath isFirstOrLastMissionDayAHoliday = createBoolean("isFirstOrLastMissionDayAHoliday");

    public final BooleanPath isHolidayInMissionAWorkingDay = createBoolean("isHolidayInMissionAWorkingDay");

    public final BooleanPath isIntervalTimeCutFromWorkingTime = createBoolean("isIntervalTimeCutFromWorkingTime");

    public final BooleanPath isLastDayBeforeEasterEntire = createBoolean("isLastDayBeforeEasterEntire");

    public final BooleanPath isLastDayBeforeXmasEntire = createBoolean("isLastDayBeforeXmasEntire");

    public final BooleanPath isLastDayOfTheYearEntire = createBoolean("isLastDayOfTheYearEntire");

    public final BooleanPath isMealTimeShorterThanMinimum = createBoolean("isMealTimeShorterThanMinimum");

    public final NumberPath<Integer> maximumOvertimeHours = createNumber("maximumOvertimeHours", Integer.class);

    public final NumberPath<Integer> maxRecoveryDaysFourNine = createNumber("maxRecoveryDaysFourNine", Integer.class);

    public final NumberPath<Integer> maxRecoveryDaysOneThree = createNumber("maxRecoveryDaysOneThree", Integer.class);

    public final BooleanPath mealTicketAssignedWithMealTimeReal = createBoolean("mealTicketAssignedWithMealTimeReal");

    public final BooleanPath mealTicketAssignedWithReasonMealTime = createBoolean("mealTicketAssignedWithReasonMealTime");

    public final NumberPath<Integer> mealTime = createNumber("mealTime", Integer.class);

    public final NumberPath<Integer> mealTimeEndHour = createNumber("mealTimeEndHour", Integer.class);

    public final NumberPath<Integer> mealTimeEndMinute = createNumber("mealTimeEndMinute", Integer.class);

    public final NumberPath<Integer> mealTimeStartHour = createNumber("mealTimeStartHour", Integer.class);

    public final NumberPath<Integer> mealTimeStartMinute = createNumber("mealTimeStartMinute", Integer.class);

    public final NumberPath<Integer> minimumRemainingTimeToHaveRecoveryDay = createNumber("minimumRemainingTimeToHaveRecoveryDay", Integer.class);

    public final NumberPath<Integer> monthExpireRecoveryDaysFourNine = createNumber("monthExpireRecoveryDaysFourNine", Integer.class);

    public final NumberPath<Integer> monthExpireRecoveryDaysOneThree = createNumber("monthExpireRecoveryDaysOneThree", Integer.class);

    public final NumberPath<Integer> monthExpiryVacationPastYear = createNumber("monthExpiryVacationPastYear", Integer.class);

    public final NumberPath<Integer> monthOfPatron = createNumber("monthOfPatron", Integer.class);

    public final NumberPath<Integer> numberOfViewingCoupleColumn = createNumber("numberOfViewingCoupleColumn", Integer.class);

    public final StringPath passwordToPresence = createString("passwordToPresence");

    public final StringPath pathToSavePresenceSituation = createString("pathToSavePresenceSituation");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final EnumPath<models.enumerate.ResidualWithPastYear> residual = createEnum("residual", models.enumerate.ResidualWithPastYear.class);

    public final NumberPath<Integer> seatCode = createNumber("seatCode", Integer.class);

    public final StringPath textForMonthlySituation = createString("textForMonthlySituation");

    public final StringPath urlToPresence = createString("urlToPresence");

    public final StringPath userToPresence = createString("userToPresence");

    public final ListPath<models.WebStampingAddress, QWebStampingAddress> webStampingAddress = this.<models.WebStampingAddress, QWebStampingAddress>createList("webStampingAddress", models.WebStampingAddress.class, QWebStampingAddress.class, PathInits.DIRECT2);

    public final NumberPath<Integer> workingTime = createNumber("workingTime", Integer.class);

    public final NumberPath<Integer> workingTimeToHaveMealTicket = createNumber("workingTimeToHaveMealTicket", Integer.class);

    public QConfiguration(String variable) {
        super(Configuration.class, forVariable(variable));
    }

    public QConfiguration(Path<? extends Configuration> path) {
        super(path.getType(), path.getMetadata());
    }

    public QConfiguration(PathMetadata<?> metadata) {
        super(Configuration.class, metadata);
    }

}

