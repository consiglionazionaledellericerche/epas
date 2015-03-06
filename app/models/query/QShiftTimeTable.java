package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ShiftTimeTable;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QShiftTimeTable is a Querydsl query type for ShiftTimeTable
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QShiftTimeTable extends EntityPathBase<ShiftTimeTable> {

    private static final long serialVersionUID = -144019773L;

    public static final QShiftTimeTable shiftTimeTable = new QShiftTimeTable("shiftTimeTable");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final TimePath<org.joda.time.LocalTime> endAfternoon = createTime("endAfternoon", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endAfternoonLunchTime = createTime("endAfternoonLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endMorning = createTime("endMorning", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endMorningLunchTime = createTime("endMorningLunchTime", org.joda.time.LocalTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> paidMinutes = createNumber("paidMinutes", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.ShiftType, QShiftType> shiftTypes = this.<models.ShiftType, QShiftType>createList("shiftTypes", models.ShiftType.class, QShiftType.class, PathInits.DIRECT2);

    public final TimePath<org.joda.time.LocalTime> startAfternoon = createTime("startAfternoon", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startAfternoonLunchTime = createTime("startAfternoonLunchTime", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startMorning = createTime("startMorning", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> startMorningLunchTime = createTime("startMorningLunchTime", org.joda.time.LocalTime.class);

    public final NumberPath<Integer> totalWorkMinutes = createNumber("totalWorkMinutes", Integer.class);

    public QShiftTimeTable(String variable) {
        super(ShiftTimeTable.class, forVariable(variable));
    }

    public QShiftTimeTable(Path<? extends ShiftTimeTable> path) {
        super(path.getType(), path.getMetadata());
    }

    public QShiftTimeTable(PathMetadata<?> metadata) {
        super(ShiftTimeTable.class, metadata);
    }

}

