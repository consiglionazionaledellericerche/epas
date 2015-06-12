package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.ConfYear;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QConfYear is a Querydsl query type for ConfYear
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QConfYear extends EntityPathBase<ConfYear> {

    private static final long serialVersionUID = 2027912357L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConfYear confYear = new QConfYear("confYear");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath field = createString("field");

    public final StringPath fieldValue = createString("fieldValue");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QConfYear(String variable) {
        this(ConfYear.class, forVariable(variable), INITS);
    }

    public QConfYear(Path<? extends ConfYear> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QConfYear(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QConfYear(PathMetadata<?> metadata, PathInits inits) {
        this(ConfYear.class, metadata, inits);
    }

    public QConfYear(Class<? extends ConfYear> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

