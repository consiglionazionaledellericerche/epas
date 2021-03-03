package models.flows.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.flows.CompetenceRequestEvent;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCompetenceRequestEvent is a Querydsl query type for CompetenceRequestEvent
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCompetenceRequestEvent extends EntityPathBase<CompetenceRequestEvent> {

    private static final long serialVersionUID = 1760135137L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCompetenceRequestEvent competenceRequestEvent = new QCompetenceRequestEvent("competenceRequestEvent");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QCompetenceRequest competenceRequest;

    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = createDateTime("createdAt", org.joda.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final EnumPath<models.flows.enumerate.CompetenceRequestEventType> eventType = createEnum("eventType", models.flows.enumerate.CompetenceRequestEventType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final models.query.QUser owner;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QCompetenceRequestEvent(String variable) {
        this(CompetenceRequestEvent.class, forVariable(variable), INITS);
    }

    public QCompetenceRequestEvent(Path<? extends CompetenceRequestEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCompetenceRequestEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCompetenceRequestEvent(PathMetadata metadata, PathInits inits) {
        this(CompetenceRequestEvent.class, metadata, inits);
    }

    public QCompetenceRequestEvent(Class<? extends CompetenceRequestEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceRequest = inits.isInitialized("competenceRequest") ? new QCompetenceRequest(forProperty("competenceRequest"), inits.get("competenceRequest")) : null;
        this.owner = inits.isInitialized("owner") ? new models.query.QUser(forProperty("owner"), inits.get("owner")) : null;
    }

}

