package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonShift;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonShift is a Querydsl query type for PersonShift
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonShift extends EntityPathBase<PersonShift> {

    private static final long serialVersionUID = 1443244169L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShift personShift = new QPersonShift("personShift");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath jolly = createBoolean("jolly");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final ListPath<models.PersonShiftDay, QPersonShiftDay> personShiftDays = this.<models.PersonShiftDay, QPersonShiftDay>createList("personShiftDays", models.PersonShiftDay.class, QPersonShiftDay.class, PathInits.DIRECT2);

    public final ListPath<models.PersonShiftShiftType, QPersonShiftShiftType> personShiftShiftTypes = this.<models.PersonShiftShiftType, QPersonShiftShiftType>createList("personShiftShiftTypes", models.PersonShiftShiftType.class, QPersonShiftShiftType.class, PathInits.DIRECT2);

    public QPersonShift(String variable) {
        this(PersonShift.class, forVariable(variable), INITS);
    }

    public QPersonShift(Path<? extends PersonShift> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShift(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShift(PathMetadata<?> metadata, PathInits inits) {
        this(PersonShift.class, metadata, inits);
    }

    public QPersonShift(Class<? extends PersonShift> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

