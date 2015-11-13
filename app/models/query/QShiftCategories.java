package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;
import models.ShiftCategories;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


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

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

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
        this.supervisor = inits.isInitialized("supervisor") ? new QPerson(forProperty("supervisor"), inits.get("supervisor")) : null;
    }

}

