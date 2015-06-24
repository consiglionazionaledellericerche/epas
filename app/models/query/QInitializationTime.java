package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.InitializationTime;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QInitializationTime is a Querydsl query type for InitializationTime
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QInitializationTime extends EntityPathBase<InitializationTime> {

    private static final long serialVersionUID = 1450202545L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInitializationTime initializationTime = new QInitializationTime("initializationTime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> permissionUsed = createNumber("permissionUsed", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> recoveryDayUsed = createNumber("recoveryDayUsed", Integer.class);

    public final NumberPath<Integer> residualMinutesCurrentYear = createNumber("residualMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> residualMinutesPastYear = createNumber("residualMinutesPastYear", Integer.class);

    public final NumberPath<Integer> vacationCurrentYearUsed = createNumber("vacationCurrentYearUsed", Integer.class);

    public final NumberPath<Integer> vacationLastYearUsed = createNumber("vacationLastYearUsed", Integer.class);

    public QInitializationTime(String variable) {
        this(InitializationTime.class, forVariable(variable), INITS);
    }

    public QInitializationTime(Path<? extends InitializationTime> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationTime(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QInitializationTime(PathMetadata<?> metadata, PathInits inits) {
        this(InitializationTime.class, metadata, inits);
    }

    public QInitializationTime(Class<? extends InitializationTime> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

