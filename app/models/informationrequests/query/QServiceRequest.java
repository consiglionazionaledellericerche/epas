package models.informationrequests.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.informationrequests.ServiceRequest;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QServiceRequest is a Querydsl query type for ServiceRequest
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QServiceRequest extends EntityPathBase<ServiceRequest> {

    private static final long serialVersionUID = 215801084L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QServiceRequest serviceRequest = new QServiceRequest("serviceRequest");

    public final models.base.query.QInformationRequest _super;

    public final DatePath<java.time.LocalDate> day = createDate("day", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> endTo = createTime("endTo", java.time.LocalTime.class);

    //inherited
    public final SimplePath<Object> entityId;

    //inherited
    public final ListPath<models.informationrequests.InformationRequestEvent, QInformationRequestEvent> events;

    //inherited
    public final BooleanPath flowEnded;

    //inherited
    public final BooleanPath flowStarted;

    //inherited
    public final NumberPath<Long> id;

    //inherited
    public final EnumPath<models.enumerate.InformationType> informationType;

    //inherited
    public final BooleanPath officeHeadApprovalRequired;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> officeHeadApproved;

    //inherited
    public final BooleanPath persistent;

    // inherited
    public final models.query.QPerson person;

    public final StringPath reason = createString("reason");

    public final TimePath<java.time.LocalTime> startAt = createTime("startAt", java.time.LocalTime.class);

    //inherited
    public final NumberPath<Integer> version;

    public QServiceRequest(String variable) {
        this(ServiceRequest.class, forVariable(variable), INITS);
    }

    public QServiceRequest(Path<? extends ServiceRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QServiceRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QServiceRequest(PathMetadata metadata, PathInits inits) {
        this(ServiceRequest.class, metadata, inits);
    }

    public QServiceRequest(Class<? extends ServiceRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new models.base.query.QInformationRequest(type, metadata, inits);
        this.entityId = _super.entityId;
        this.events = _super.events;
        this.flowEnded = _super.flowEnded;
        this.flowStarted = _super.flowStarted;
        this.id = _super.id;
        this.informationType = _super.informationType;
        this.officeHeadApprovalRequired = _super.officeHeadApprovalRequired;
        this.officeHeadApproved = _super.officeHeadApproved;
        this.persistent = _super.persistent;
        this.person = _super.person;
        this.version = _super.version;
    }

}

