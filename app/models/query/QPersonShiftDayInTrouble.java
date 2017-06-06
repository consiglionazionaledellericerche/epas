package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonShiftDayInTrouble;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonShiftDayInTrouble is a Querydsl query type for PersonShiftDayInTrouble
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonShiftDayInTrouble extends EntityPathBase<PersonShiftDayInTrouble> {

    private static final long serialVersionUID = -1788708865L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShiftDayInTrouble personShiftDayInTrouble = new QPersonShiftDayInTrouble("personShiftDayInTrouble");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.enumerate.ShiftTroubles> cause = createEnum("cause", models.enumerate.ShiftTroubles.class);

    public final BooleanPath emailSent = createBoolean("emailSent");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonShiftDay personShiftDay;

    public QPersonShiftDayInTrouble(String variable) {
        this(PersonShiftDayInTrouble.class, forVariable(variable), INITS);
    }

    public QPersonShiftDayInTrouble(Path<? extends PersonShiftDayInTrouble> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftDayInTrouble(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonShiftDayInTrouble(PathMetadata<?> metadata, PathInits inits) {
        this(PersonShiftDayInTrouble.class, metadata, inits);
    }

    public QPersonShiftDayInTrouble(Class<? extends PersonShiftDayInTrouble> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personShiftDay = inits.isInitialized("personShiftDay") ? new QPersonShiftDay(forProperty("personShiftDay"), inits.get("personShiftDay")) : null;
    }

}

