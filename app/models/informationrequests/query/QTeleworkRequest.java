package models.informationrequests.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.informationrequests.TeleworkRequest;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTeleworkRequest is a Querydsl query type for TeleworkRequest
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTeleworkRequest extends EntityPathBase<TeleworkRequest> {

    private static final long serialVersionUID = 291785138L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTeleworkRequest teleworkRequest = new QTeleworkRequest("teleworkRequest");

    public final models.base.query.QInformationRequest _super;

    //inherited
    public final BooleanPath administrativeApprovalRequired;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> administrativeApproved;

    public final StringPath context = createString("context");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> endTo;

    //inherited
    public final SimplePath<Object> entityId;

    //inherited
    public final ListPath<models.informationrequests.InformationRequestEvent, QInformationRequestEvent> events;

    //inherited
    public final BooleanPath flowEnded;

    //inherited
    public final BooleanPath flowStarted;

    //inherited
    public final BooleanPath fullyApproved;

    //inherited
    public final NumberPath<Long> id;

    //inherited
    public final EnumPath<models.enumerate.InformationType> informationType;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath officeHeadApprovalRequired;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> officeHeadApproved;

    //inherited
    public final BooleanPath persistent;

    // inherited
    public final models.query.QPerson person;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> startAt;

    //inherited
    public final NumberPath<Integer> version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QTeleworkRequest(String variable) {
        this(TeleworkRequest.class, forVariable(variable), INITS);
    }

    public QTeleworkRequest(Path<? extends TeleworkRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTeleworkRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTeleworkRequest(PathMetadata metadata, PathInits inits) {
        this(TeleworkRequest.class, metadata, inits);
    }

    public QTeleworkRequest(Class<? extends TeleworkRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new models.base.query.QInformationRequest(type, metadata, inits);
        this.administrativeApprovalRequired = _super.administrativeApprovalRequired;
        this.administrativeApproved = _super.administrativeApproved;
        this.endTo = _super.endTo;
        this.entityId = _super.entityId;
        this.events = _super.events;
        this.flowEnded = _super.flowEnded;
        this.flowStarted = _super.flowStarted;
        this.fullyApproved = _super.fullyApproved;
        this.id = _super.id;
        this.informationType = _super.informationType;
        this.officeHeadApprovalRequired = _super.officeHeadApprovalRequired;
        this.officeHeadApproved = _super.officeHeadApproved;
        this.persistent = _super.persistent;
        this.person = _super.person;
        this.startAt = _super.startAt;
        this.version = _super.version;
    }

}

