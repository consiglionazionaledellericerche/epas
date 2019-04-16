package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ShiftType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShiftType is a Querydsl query type for ShiftType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QShiftType extends EntityPathBase<ShiftType> {

    private static final long serialVersionUID = 153403448L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftType shiftType = new QShiftType("shiftType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final BooleanPath allowUnpairSlots = createBoolean("allowUnpairSlots");

    public final NumberPath<Integer> breakInShift = createNumber("breakInShift", Integer.class);

    public final NumberPath<Integer> breakMaxInShift = createNumber("breakMaxInShift", Integer.class);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> entranceMaxTolerance = createNumber("entranceMaxTolerance", Integer.class);

    public final NumberPath<Integer> entranceTolerance = createNumber("entranceTolerance", Integer.class);

    public final NumberPath<Integer> exitMaxTolerance = createNumber("exitMaxTolerance", Integer.class);

    public final NumberPath<Integer> exitTolerance = createNumber("exitTolerance", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> maxToleranceAllowed = createNumber("maxToleranceAllowed", Integer.class);

    public final SetPath<models.ShiftTypeMonth, QShiftTypeMonth> monthsStatus = this.<models.ShiftTypeMonth, QShiftTypeMonth>createSet("monthsStatus", models.ShiftTypeMonth.class, QShiftTypeMonth.class, PathInits.DIRECT2);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonShiftDay, QPersonShiftDay> personShiftDays = this.<models.PersonShiftDay, QPersonShiftDay>createList("personShiftDays", models.PersonShiftDay.class, QPersonShiftDay.class, PathInits.DIRECT2);

    public final ListPath<models.PersonShiftShiftType, QPersonShiftShiftType> personShiftShiftTypes = this.<models.PersonShiftShiftType, QPersonShiftShiftType>createList("personShiftShiftTypes", models.PersonShiftShiftType.class, QPersonShiftShiftType.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftCancelled, QShiftCancelled> shiftCancelled = this.<models.ShiftCancelled, QShiftCancelled>createList("shiftCancelled", models.ShiftCancelled.class, QShiftCancelled.class, PathInits.DIRECT2);

    public final QShiftCategories shiftCategories;

    public final QShiftTimeTable shiftTimeTable;

    public final StringPath type = createString("type");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QShiftType(String variable) {
        this(ShiftType.class, forVariable(variable), INITS);
    }

    public QShiftType(Path<? extends ShiftType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShiftType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShiftType(PathMetadata metadata, PathInits inits) {
        this(ShiftType.class, metadata, inits);
    }

    public QShiftType(Class<? extends ShiftType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.shiftCategories = inits.isInitialized("shiftCategories") ? new QShiftCategories(forProperty("shiftCategories"), inits.get("shiftCategories")) : null;
        this.shiftTimeTable = inits.isInitialized("shiftTimeTable") ? new QShiftTimeTable(forProperty("shiftTimeTable"), inits.get("shiftTimeTable")) : null;
    }

}

