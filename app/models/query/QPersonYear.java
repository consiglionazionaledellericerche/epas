package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonYear;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonYear is a Querydsl query type for PersonYear
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonYear extends EntityPathBase<PersonYear> {

    private static final long serialVersionUID = -784552106L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonYear personYear = new QPersonYear("personYear");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> remainingMinutes = createNumber("remainingMinutes", Integer.class);

    public final NumberPath<Integer> remainingVacationDays = createNumber("remainingVacationDays", Integer.class);

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QPersonYear(String variable) {
        this(PersonYear.class, forVariable(variable), INITS);
    }

    public QPersonYear(Path<? extends PersonYear> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonYear(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonYear(PathMetadata<?> metadata, PathInits inits) {
        this(PersonYear.class, metadata, inits);
    }

    public QPersonYear(Class<? extends PersonYear> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

