package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ShiftTimeTable;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShiftTimeTable is a Querydsl query type for ShiftTimeTable
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShiftTimeTable extends EntityPathBase<ShiftTimeTable> {

    private static final long serialVersionUID = -144019773L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftTimeTable shiftTimeTable = new QShiftTimeTable("shiftTimeTable");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.enumerate.CalculationType> calculationType = createEnum("calculationType", models.enumerate.CalculationType.class);

    public final TimePath<org.joda.time.LocalTime> endAfternoon = createTime("endAfternoon", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endAfternoonLunchTime = createTime("endAfternoonLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endEvening = createTime("endEvening", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endEveningLunchTime = createTime("endEveningLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endMorning = createTime("endMorning", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endMorningLunchTime = createTime("endMorningLunchTime", org.joda.time.LocalTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    public final NumberPath<Integer> paidMinutesAfternoon = createNumber("paidMinutesAfternoon", Integer.class);

    public final NumberPath<Integer> paidMinutesMorning = createNumber("paidMinutesMorning", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.ShiftType, QShiftType> shiftTypes = this.<models.ShiftType, QShiftType>createList("shiftTypes", models.ShiftType.class, QShiftType.class, PathInits.DIRECT2);

    public final TimePath<org.joda.time.LocalTime> startAfternoon = createTime("startAfternoon", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startAfternoonLunchTime = createTime("startAfternoonLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startEvening = createTime("startEvening", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startEveningLunchTime = createTime("startEveningLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startMorning = createTime("startMorning", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startMorningLunchTime = createTime("startMorningLunchTime", org.joda.time.LocalTime.class);

    public final NumberPath<Integer> totalWorkMinutes = createNumber("totalWorkMinutes", Integer.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QShiftTimeTable(String variable) {
        this(ShiftTimeTable.class, forVariable(variable), INITS);
    }

    public QShiftTimeTable(Path<? extends ShiftTimeTable> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShiftTimeTable(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShiftTimeTable(PathMetadata metadata, PathInits inits) {
        this(ShiftTimeTable.class, metadata, inits);
    }

    public QShiftTimeTable(Class<? extends ShiftTimeTable> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

