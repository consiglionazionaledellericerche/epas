package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ShiftCategories;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QShiftCategories is a Querydsl query type for ShiftCategories
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QShiftCategories extends EntityPathBase<ShiftCategories> {

    private static final long serialVersionUID = 322977946L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShiftCategories shiftCategories = new QShiftCategories("shiftCategories");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson supervisor;

    public QShiftCategories(String variable) {
        this(ShiftCategories.class, forVariable(variable), INITS);
    }

    public QShiftCategories(Path<? extends ShiftCategories> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftCategories(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QShiftCategories(PathMetadata<?> metadata, PathInits inits) {
        this(ShiftCategories.class, metadata, inits);
    }

    public QShiftCategories(Class<? extends ShiftCategories> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.supervisor = inits.isInitialized("supervisor") ? new QPerson(forProperty("supervisor"), inits.get("supervisor")) : null;
    }

}

