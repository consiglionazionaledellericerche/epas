package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ShiftType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QShiftType is a Querydsl query type for ShiftType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QShiftType extends EntityPathBase<ShiftType> {

    private static final long serialVersionUID = 153403448;

    public static final QShiftType shiftType = new QShiftType("shiftType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonShiftDay, QPersonShiftDay> personShiftDays = this.<models.PersonShiftDay, QPersonShiftDay>createList("personShiftDays", models.PersonShiftDay.class, QPersonShiftDay.class, PathInits.DIRECT2);

    public final ListPath<models.PersonShiftShiftType, QPersonShiftShiftType> personShiftShiftTypes = this.<models.PersonShiftShiftType, QPersonShiftShiftType>createList("personShiftShiftTypes", models.PersonShiftShiftType.class, QPersonShiftShiftType.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftCancelled, QShiftCancelled> shiftCancelled = this.<models.ShiftCancelled, QShiftCancelled>createList("shiftCancelled", models.ShiftCancelled.class, QShiftCancelled.class, PathInits.DIRECT2);

    public final StringPath type = createString("type");

    public QShiftType(String variable) {
        super(ShiftType.class, forVariable(variable));
    }

    public QShiftType(Path<? extends ShiftType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QShiftType(PathMetadata<?> metadata) {
        super(ShiftType.class, metadata);
    }

}

