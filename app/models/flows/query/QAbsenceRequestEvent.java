package models.flows.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.flows.AbsenceRequestEvent;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsenceRequestEvent is a Querydsl query type for AbsenceRequestEvent
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsenceRequestEvent extends EntityPathBase<AbsenceRequestEvent> {

    private static final long serialVersionUID = 1496759067L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceRequestEvent absenceRequestEvent = new QAbsenceRequestEvent("absenceRequestEvent");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QAbsenceRequest absenceRequest;

    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = createDateTime("createdAt", org.joda.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final EnumPath<models.flows.enumerate.AbsenceRequestEventType> eventType = createEnum("eventType", models.flows.enumerate.AbsenceRequestEventType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final models.query.QUser owner;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAbsenceRequestEvent(String variable) {
        this(AbsenceRequestEvent.class, forVariable(variable), INITS);
    }

    public QAbsenceRequestEvent(Path<? extends AbsenceRequestEvent> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceRequestEvent(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceRequestEvent(PathMetadata<?> metadata, PathInits inits) {
        this(AbsenceRequestEvent.class, metadata, inits);
    }

    public QAbsenceRequestEvent(Class<? extends AbsenceRequestEvent> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.absenceRequest = inits.isInitialized("absenceRequest") ? new QAbsenceRequest(forProperty("absenceRequest"), inits.get("absenceRequest")) : null;
        this.owner = inits.isInitialized("owner") ? new models.query.QUser(forProperty("owner"), inits.get("owner")) : null;
    }

}

