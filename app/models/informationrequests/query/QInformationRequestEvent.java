package models.informationrequests.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.informationrequests.InformationRequestEvent;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInformationRequestEvent is a Querydsl query type for InformationRequestEvent
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QInformationRequestEvent extends EntityPathBase<InformationRequestEvent> {

    private static final long serialVersionUID = -1698121739L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInformationRequestEvent informationRequestEvent = new QInformationRequestEvent("informationRequestEvent");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = createDateTime("createdAt", org.joda.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final EnumPath<models.flows.enumerate.InformationRequestEventType> eventType = createEnum("eventType", models.flows.enumerate.InformationRequestEventType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final models.base.query.QInformationRequest informationRequest;

    public final models.query.QUser owner;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QInformationRequestEvent(String variable) {
        this(InformationRequestEvent.class, forVariable(variable), INITS);
    }

    public QInformationRequestEvent(Path<? extends InformationRequestEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInformationRequestEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInformationRequestEvent(PathMetadata metadata, PathInits inits) {
        this(InformationRequestEvent.class, metadata, inits);
    }

    public QInformationRequestEvent(Class<? extends InformationRequestEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.informationRequest = inits.isInitialized("informationRequest") ? new models.base.query.QInformationRequest(forProperty("informationRequest"), inits.get("informationRequest")) : null;
        this.owner = inits.isInitialized("owner") ? new models.query.QUser(forProperty("owner"), inits.get("owner")) : null;
    }

}

