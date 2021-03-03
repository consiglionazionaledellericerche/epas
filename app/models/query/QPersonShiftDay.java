package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonShiftDay;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonShiftDay is a Querydsl query type for PersonShiftDay
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonShiftDay extends EntityPathBase<PersonShiftDay> {

    private static final long serialVersionUID = -1230493101L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonShiftDay personShiftDay = new QPersonShiftDay("personShiftDay");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> exceededThresholds = createNumber("exceededThresholds", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOrganizationShiftSlot organizationShiftSlot;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonShift personShift;

    public final EnumPath<models.enumerate.ShiftSlot> shiftSlot = createEnum("shiftSlot", models.enumerate.ShiftSlot.class);

    public final QShiftType shiftType;

    public final SetPath<models.PersonShiftDayInTrouble, QPersonShiftDayInTrouble> troubles = this.<models.PersonShiftDayInTrouble, QPersonShiftDayInTrouble>createSet("troubles", models.PersonShiftDayInTrouble.class, QPersonShiftDayInTrouble.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonShiftDay(String variable) {
        this(PersonShiftDay.class, forVariable(variable), INITS);
    }

    public QPersonShiftDay(Path<? extends PersonShiftDay> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonShiftDay(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonShiftDay(PathMetadata metadata, PathInits inits) {
        this(PersonShiftDay.class, metadata, inits);
    }

    public QPersonShiftDay(Class<? extends PersonShiftDay> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organizationShiftSlot = inits.isInitialized("organizationShiftSlot") ? new QOrganizationShiftSlot(forProperty("organizationShiftSlot"), inits.get("organizationShiftSlot")) : null;
        this.personShift = inits.isInitialized("personShift") ? new QPersonShift(forProperty("personShift"), inits.get("personShift")) : null;
        this.shiftType = inits.isInitialized("shiftType") ? new QShiftType(forProperty("shiftType"), inits.get("shiftType")) : null;
    }

}

