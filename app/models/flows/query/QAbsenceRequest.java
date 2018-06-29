package models.flows.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.flows.AbsenceRequest;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QAbsenceRequest is a Querydsl query type for AbsenceRequest
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QAbsenceRequest extends EntityPathBase<AbsenceRequest> {

    private static final long serialVersionUID = 1310860991L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceRequest absenceRequest = new QAbsenceRequest("absenceRequest");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final BooleanPath administrativeApprovalRequired = createBoolean("administrativeApprovalRequired");

    public final DatePath<org.joda.time.LocalDate> administrativeApproved = createDate("administrativeApproved", org.joda.time.LocalDate.class);

    public final SimplePath<play.db.jpa.Blob> attachment = createSimple("attachment", play.db.jpa.Blob.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final DateTimePath<org.joda.time.LocalDateTime> endTo = createDateTime("endTo", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.flows.AbsenceRequestEvent, QAbsenceRequestEvent> events = this.<models.flows.AbsenceRequestEvent, QAbsenceRequestEvent>createList("events", models.flows.AbsenceRequestEvent.class, QAbsenceRequestEvent.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath managerApprovalRequired = createBoolean("managerApprovalRequired");

    public final DatePath<org.joda.time.LocalDate> managerApproved = createDate("managerApproved", org.joda.time.LocalDate.class);

    public final BooleanPath officeHeadApprovalRequired = createBoolean("officeHeadApprovalRequired");

    public final DatePath<org.joda.time.LocalDate> officeHeadApproved = createDate("officeHeadApproved", org.joda.time.LocalDate.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPerson person;

    public final DateTimePath<org.joda.time.LocalDateTime> startAt = createDateTime("startAt", org.joda.time.LocalDateTime.class);

    public final EnumPath<models.flows.enumerate.AbsenceRequestType> type = createEnum("type", models.flows.enumerate.AbsenceRequestType.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAbsenceRequest(String variable) {
        this(AbsenceRequest.class, forVariable(variable), INITS);
    }

    public QAbsenceRequest(Path<? extends AbsenceRequest> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceRequest(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QAbsenceRequest(PathMetadata<?> metadata, PathInits inits) {
        this(AbsenceRequest.class, metadata, inits);
    }

    public QAbsenceRequest(Class<? extends AbsenceRequest> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

