package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ShiftCancelled;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShiftCancelled is a Querydsl query type for ShiftCancelled
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShiftCancelled extends EntityPathBase<ShiftCancelled> {

    private static final long serialVersionUID = 306636563L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftCancelled shiftCancelled = new QShiftCancelled("shiftCancelled");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QShiftType type;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QShiftCancelled(String variable) {
        this(ShiftCancelled.class, forVariable(variable), INITS);
    }

    public QShiftCancelled(Path<? extends ShiftCancelled> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShiftCancelled(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShiftCancelled(PathMetadata metadata, PathInits inits) {
        this(ShiftCancelled.class, metadata, inits);
    }

    public QShiftCancelled(Class<? extends ShiftCancelled> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.type = inits.isInitialized("type") ? new QShiftType(forProperty("type"), inits.get("type")) : null;
    }

}

