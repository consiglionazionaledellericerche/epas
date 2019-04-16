package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonShiftShiftType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonShiftShiftType is a Querydsl query type for PersonShiftShiftType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonShiftShiftType extends EntityPathBase<PersonShiftShiftType> {

    private static final long serialVersionUID = -228204461L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShiftShiftType personShiftShiftType = new QPersonShiftShiftType("personShiftShiftType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath jolly = createBoolean("jolly");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonShift personShift;

    public final QShiftType shiftType;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonShiftShiftType(String variable) {
        this(PersonShiftShiftType.class, forVariable(variable), INITS);
    }

    public QPersonShiftShiftType(Path<? extends PersonShiftShiftType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonShiftShiftType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonShiftShiftType(PathMetadata metadata, PathInits inits) {
        this(PersonShiftShiftType.class, metadata, inits);
    }

    public QPersonShiftShiftType(Class<? extends PersonShiftShiftType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.shiftType = inits.isInitialized("shiftType") ? new QShiftType(forProperty("shiftType"), inits.get("shiftType")) : null;
    }

}

