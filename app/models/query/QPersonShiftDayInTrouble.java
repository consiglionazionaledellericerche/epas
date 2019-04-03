package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonShiftDayInTrouble;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonShiftDayInTrouble is a Querydsl query type for PersonShiftDayInTrouble
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonShiftDayInTrouble(String variable) {
        this(PersonShiftDayInTrouble.class, forVariable(variable), INITS);
    }

    public QPersonShiftDayInTrouble(Path<? extends PersonShiftDayInTrouble> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonShiftDayInTrouble(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonShiftDayInTrouble(PathMetadata metadata, PathInits inits) {
        this(PersonShiftDayInTrouble.class, metadata, inits);
    }

    public QPersonShiftDayInTrouble(Class<? extends PersonShiftDayInTrouble> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.personShiftDay = inits.isInitialized("personShiftDay") ? new QPersonShiftDay(forProperty("personShiftDay"), inits.get("personShiftDay")) : null;
    }

}

