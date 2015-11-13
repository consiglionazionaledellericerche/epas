package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import models.PersonWorkingTimeType;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QPersonWorkingTimeType is a Querydsl query type for PersonWorkingTimeType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonWorkingTimeType extends EntityPathBase<PersonWorkingTimeType> {

    private static final long serialVersionUID = 1337300287L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonWorkingTimeType personWorkingTimeType = new QPersonWorkingTimeType("personWorkingTimeType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final QWorkingTimeType workingTimeType;

    public QPersonWorkingTimeType(String variable) {
        this(PersonWorkingTimeType.class, forVariable(variable), INITS);
    }

    public QPersonWorkingTimeType(Path<? extends PersonWorkingTimeType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonWorkingTimeType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonWorkingTimeType(PathMetadata<?> metadata, PathInits inits) {
        this(PersonWorkingTimeType.class, metadata, inits);
    }

    public QPersonWorkingTimeType(Class<? extends PersonWorkingTimeType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
        this.workingTimeType = inits.isInitialized("workingTimeType") ? new QWorkingTimeType(forProperty("workingTimeType"), inits.get("workingTimeType")) : null;
    }

}

