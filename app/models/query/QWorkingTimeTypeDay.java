package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.WorkingTimeTypeDay;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWorkingTimeTypeDay is a Querydsl query type for WorkingTimeTypeDay
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QWorkingTimeTypeDay extends EntityPathBase<WorkingTimeTypeDay> {

    private static final long serialVersionUID = 1247746216L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWorkingTimeTypeDay workingTimeTypeDay = new QWorkingTimeTypeDay("workingTimeTypeDay");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> breakTicketTime = createNumber("breakTicketTime", Integer.class);

    public final NumberPath<Integer> dayOfWeek = createNumber("dayOfWeek", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath holiday = createBoolean("holiday");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> mealTicketTime = createNumber("mealTicketTime", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> ticketAfternoonThreshold = createNumber("ticketAfternoonThreshold", Integer.class);

    public final NumberPath<Integer> ticketAfternoonWorkingTime = createNumber("ticketAfternoonWorkingTime", Integer.class);

    public final NumberPath<Integer> timeMealFrom = createNumber("timeMealFrom", Integer.class);

    public final NumberPath<Integer> timeMealTo = createNumber("timeMealTo", Integer.class);

    public final NumberPath<Integer> timeSlotEntranceFrom = createNumber("timeSlotEntranceFrom", Integer.class);

    public final NumberPath<Integer> timeSlotEntranceTo = createNumber("timeSlotEntranceTo", Integer.class);

    public final NumberPath<Integer> timeSlotExitFrom = createNumber("timeSlotExitFrom", Integer.class);

    public final NumberPath<Integer> timeSlotExitTo = createNumber("timeSlotExitTo", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> workingTime = createNumber("workingTime", Integer.class);

    public final QWorkingTimeType workingTimeType;

    public QWorkingTimeTypeDay(String variable) {
        this(WorkingTimeTypeDay.class, forVariable(variable), INITS);
    }

    public QWorkingTimeTypeDay(Path<? extends WorkingTimeTypeDay> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWorkingTimeTypeDay(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWorkingTimeTypeDay(PathMetadata metadata, PathInits inits) {
        this(WorkingTimeTypeDay.class, metadata, inits);
    }

    public QWorkingTimeTypeDay(Class<? extends WorkingTimeTypeDay> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.workingTimeType = inits.isInitialized("workingTimeType") ? new QWorkingTimeType(forProperty("workingTimeType"), inits.get("workingTimeType")) : null;
    }

}

