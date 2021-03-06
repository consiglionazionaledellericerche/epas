package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ShiftTypeMonth;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShiftTypeMonth is a Querydsl query type for ShiftTypeMonth
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShiftTypeMonth extends EntityPathBase<ShiftTypeMonth> {

    private static final long serialVersionUID = 332649768L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftTypeMonth shiftTypeMonth = new QShiftTypeMonth("shiftTypeMonth");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final BooleanPath approved = createBoolean("approved");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QShiftType shiftType;

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final ComparablePath<org.joda.time.YearMonth> yearMonth = createComparable("yearMonth", org.joda.time.YearMonth.class);

    public QShiftTypeMonth(String variable) {
        this(ShiftTypeMonth.class, forVariable(variable), INITS);
    }

    public QShiftTypeMonth(Path<? extends ShiftTypeMonth> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShiftTypeMonth(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShiftTypeMonth(PathMetadata metadata, PathInits inits) {
        this(ShiftTypeMonth.class, metadata, inits);
    }

    public QShiftTypeMonth(Class<? extends ShiftTypeMonth> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.shiftType = inits.isInitialized("shiftType") ? new QShiftType(forProperty("shiftType"), inits.get("shiftType")) : null;
    }

}

