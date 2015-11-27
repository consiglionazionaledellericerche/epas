package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.ConfGeneral;


/**
 * QConfGeneral is a Querydsl query type for ConfGeneral
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QConfGeneral extends EntityPathBase<ConfGeneral> {

    private static final long serialVersionUID = 1743575936L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QConfGeneral confGeneral = new QConfGeneral("confGeneral");

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

    public QConfGeneral(String variable) {
        this(ConfGeneral.class, forVariable(variable), INITS);
    }

    public QConfGeneral(Path<? extends ConfGeneral> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QConfGeneral(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QConfGeneral(PathMetadata<?> metadata, PathInits inits) {
        this(ConfGeneral.class, metadata, inits);
    }

    public QConfGeneral(Class<? extends ConfGeneral> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

