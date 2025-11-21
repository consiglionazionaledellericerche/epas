package models.flows.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.flows.AbsenceRequest;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAbsenceRequest is a Querydsl query type for AbsenceRequest
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QAbsenceRequest extends EntityPathBase<AbsenceRequest> {

    private static final long serialVersionUID = 1310860991L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAbsenceRequest absenceRequest = new QAbsenceRequest("absenceRequest");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final StringPath absenceCode = createString("absenceCode");

    public final BooleanPath administrativeApprovalRequired = createBoolean("administrativeApprovalRequired");

    public final DateTimePath<org.joda.time.LocalDateTime> administrativeApproved = createDateTime("administrativeApproved", org.joda.time.LocalDateTime.class);

    public final SimplePath<play.db.jpa.Blob> attachment = createSimple("attachment", play.db.jpa.Blob.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<org.joda.time.LocalDateTime> endTo = createDateTime("endTo", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.flows.AbsenceRequestEvent, QAbsenceRequestEvent> events = this.<models.flows.AbsenceRequestEvent, QAbsenceRequestEvent>createList("events", models.flows.AbsenceRequestEvent.class, QAbsenceRequestEvent.class, PathInits.DIRECT2);

    public final BooleanPath flowEnded = createBoolean("flowEnded");

    public final BooleanPath flowStarted = createBoolean("flowStarted");

    public final BooleanPath fullyApproved = createBoolean("fullyApproved");

    public final NumberPath<Integer> hours = createNumber("hours", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath managerApprovalRequired = createBoolean("managerApprovalRequired");

    public final DateTimePath<org.joda.time.LocalDateTime> managerApproved = createDateTime("managerApproved", org.joda.time.LocalDateTime.class);

    public final NumberPath<Integer> minutes = createNumber("minutes", Integer.class);

    public final StringPath note = createString("note");

    public final BooleanPath officeHeadApprovalForManagerRequired = createBoolean("officeHeadApprovalForManagerRequired");

    public final BooleanPath officeHeadApprovalRequired = createBoolean("officeHeadApprovalRequired");

    public final DateTimePath<org.joda.time.LocalDateTime> officeHeadApproved = createDateTime("officeHeadApproved", org.joda.time.LocalDateTime.class);

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
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAbsenceRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAbsenceRequest(PathMetadata metadata, PathInits inits) {
        this(AbsenceRequest.class, metadata, inits);
    }

    public QAbsenceRequest(Class<? extends AbsenceRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

