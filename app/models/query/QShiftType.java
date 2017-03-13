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

    private static final long serialVersionUID = 153403448L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftType shiftType = new QShiftType("shiftType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> breakInShift = createNumber("breakInShift", Integer.class);

    public final BooleanPath breakInShiftEnabled = createBoolean("breakInShiftEnabled");

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> entranceTolerance = createNumber("entranceTolerance", Integer.class);

    public final NumberPath<Integer> exitTolerance = createNumber("exitTolerance", Integer.class);

    public final NumberPath<Integer> hourTolerance = createNumber("hourTolerance", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonShiftDay, QPersonShiftDay> personShiftDays = this.<models.PersonShiftDay, QPersonShiftDay>createList("personShiftDays", models.PersonShiftDay.class, QPersonShiftDay.class, PathInits.DIRECT2);

    public final ListPath<models.PersonShiftShiftType, QPersonShiftShiftType> personShiftShiftTypes = this.<models.PersonShiftShiftType, QPersonShiftShiftType>createList("personShiftShiftTypes", models.PersonShiftShiftType.class, QPersonShiftShiftType.class, PathInits.DIRECT2);

    public final ListPath<models.ShiftCancelled, QShiftCancelled> shiftCancelled = this.<models.ShiftCancelled, QShiftCancelled>createList("shiftCancelled", models.ShiftCancelled.class, QShiftCancelled.class, PathInits.DIRECT2);

    public final QShiftCategories shiftCategories;

    public final QShiftTimeTable shiftTimeTable;

    public final StringPath type = createString("type");

    public QShiftType(String variable) {
        this(ShiftType.class, forVariable(variable), INITS);
    }

    public QShiftType(Path<? extends ShiftType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftType(PathMetadata<?> metadata, PathInits inits) {
        this(ShiftType.class, metadata, inits);
    }

    public QShiftType(Class<? extends ShiftType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.shiftCategories = inits.isInitialized("shiftCategories") ? new QShiftCategories(forProperty("shiftCategories"), inits.get("shiftCategories")) : null;
        this.shiftTimeTable = inits.isInitialized("shiftTimeTable") ? new QShiftTimeTable(forProperty("shiftTimeTable")) : null;
    }

}

