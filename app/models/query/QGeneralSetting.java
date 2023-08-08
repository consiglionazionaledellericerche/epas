package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.GeneralSetting;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QGeneralSetting is a Querydsl query type for GeneralSetting
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QGeneralSetting extends EntityPathBase<GeneralSetting> {

    private static final long serialVersionUID = -1610391444L;

    public static final QGeneralSetting generalSetting = new QGeneralSetting("generalSetting");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath cookiePolicyContent = createString("cookiePolicyContent");

    public final BooleanPath cookiePolicyEnabled = createBoolean("cookiePolicyEnabled");

    public final BooleanPath enableAbsenceTopLevelAuthorization = createBoolean("enableAbsenceTopLevelAuthorization");

    public final BooleanPath enableAutoconfigCovid19 = createBoolean("enableAutoconfigCovid19");

    public final BooleanPath enableAutoconfigSmartworking = createBoolean("enableAutoconfigSmartworking");

    public final BooleanPath enableDailyPresenceForManager = createBoolean("enableDailyPresenceForManager");

    public final BooleanPath enableIllnessFlow = createBoolean("enableIllnessFlow");

    public final BooleanPath enableUniqueDailyShift = createBoolean("enableUniqueDailyShift");

    public final StringPath endDailyShift = createString("endDailyShift");

    public final StringPath endNightlyShift = createString("endNightlyShift");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath epasHelpdeskServiceEnabled = createBoolean("epasHelpdeskServiceEnabled");

    public final StringPath epasHelpdeskServiceUrl = createString("epasHelpdeskServiceUrl");

    public final BooleanPath epasServiceEnabled = createBoolean("epasServiceEnabled");

    public final StringPath epasServiceUrl = createString("epasServiceUrl");

    public final BooleanPath handleGroupsByInstitute = createBoolean("handleGroupsByInstitute");

    public final BooleanPath holidayShiftInNightToo = createBoolean("holidayShiftInNightToo");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> maxDaysInPastForRestStampings = createNumber("maxDaysInPastForRestStampings", Integer.class);

    public final BooleanPath onlyMealTicket = createBoolean("onlyMealTicket");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath regulationsEnabled = createBoolean("regulationsEnabled");

    public final BooleanPath roundingShiftQuantity = createBoolean("roundingShiftQuantity");

    public final BooleanPath saturdayHolidayShift = createBoolean("saturdayHolidayShift");

    public final StringPath startDailyShift = createString("startDailyShift");

    public final StringPath startNightlyShift = createString("startNightlyShift");

    public final BooleanPath syncBadgesEnabled = createBoolean("syncBadgesEnabled");

    public final BooleanPath syncOfficesEnabled = createBoolean("syncOfficesEnabled");

    public final BooleanPath syncPersonsEnabled = createBoolean("syncPersonsEnabled");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final BooleanPath warningInsertPerson = createBoolean("warningInsertPerson");

    public QGeneralSetting(String variable) {
        super(GeneralSetting.class, forVariable(variable));
    }

    public QGeneralSetting(Path<? extends GeneralSetting> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGeneralSetting(PathMetadata metadata) {
        super(GeneralSetting.class, metadata);
    }

}

