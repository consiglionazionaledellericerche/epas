package models.base.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.base.InformationRequest;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInformationRequest is a Querydsl query type for InformationRequest
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QInformationRequest extends EntityPathBase<InformationRequest> {

    private static final long serialVersionUID = -2018136420L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInformationRequest informationRequest = new QInformationRequest("informationRequest");

    public final QBaseModel _super = new QBaseModel(this);

    public final BooleanPath administrativeApprovalRequired = createBoolean("administrativeApprovalRequired");

    public final DateTimePath<java.time.LocalDateTime> administrativeApproved = createDateTime("administrativeApproved", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> endTo = createDateTime("endTo", java.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.informationrequests.InformationRequestEvent, models.informationrequests.query.QInformationRequestEvent> events = this.<models.informationrequests.InformationRequestEvent, models.informationrequests.query.QInformationRequestEvent>createList("events", models.informationrequests.InformationRequestEvent.class, models.informationrequests.query.QInformationRequestEvent.class, PathInits.DIRECT2);

    public final BooleanPath flowEnded = createBoolean("flowEnded");

    public final BooleanPath flowStarted = createBoolean("flowStarted");

    public final BooleanPath fullyApproved = createBoolean("fullyApproved");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<models.enumerate.InformationType> informationType = createEnum("informationType", models.enumerate.InformationType.class);

    public final BooleanPath officeHeadApprovalRequired = createBoolean("officeHeadApprovalRequired");

    public final DateTimePath<java.time.LocalDateTime> officeHeadApproved = createDateTime("officeHeadApproved", java.time.LocalDateTime.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPerson person;

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QInformationRequest(String variable) {
        this(InformationRequest.class, forVariable(variable), INITS);
    }

    public QInformationRequest(Path<? extends InformationRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInformationRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInformationRequest(PathMetadata metadata, PathInits inits) {
        this(InformationRequest.class, metadata, inits);
    }

    public QInformationRequest(Class<? extends InformationRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

