package models.flows.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.flows.CompetenceRequest;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCompetenceRequest is a Querydsl query type for CompetenceRequest
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCompetenceRequest extends EntityPathBase<CompetenceRequest> {

    private static final long serialVersionUID = -1125648839L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCompetenceRequest competenceRequest = new QCompetenceRequest("competenceRequest");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDateToAsk = createDate("beginDateToAsk", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> beginDateToGive = createDate("beginDateToGive", org.joda.time.LocalDate.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath employeeApprovalRequired = createBoolean("employeeApprovalRequired");

    public final DateTimePath<org.joda.time.LocalDateTime> employeeApproved = createDateTime("employeeApproved", org.joda.time.LocalDateTime.class);

    public final DatePath<org.joda.time.LocalDate> endDateToAsk = createDate("endDateToAsk", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDateToGive = createDate("endDateToGive", org.joda.time.LocalDate.class);

    public final DateTimePath<org.joda.time.LocalDateTime> endTo = createDateTime("endTo", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.flows.CompetenceRequestEvent, QCompetenceRequestEvent> events = this.<models.flows.CompetenceRequestEvent, QCompetenceRequestEvent>createList("events", models.flows.CompetenceRequestEvent.class, QCompetenceRequestEvent.class, PathInits.DIRECT2);

    public final BooleanPath flowEnded = createBoolean("flowEnded");

    public final BooleanPath flowStarted = createBoolean("flowStarted");

    public final BooleanPath fullyApproved = createBoolean("fullyApproved");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    public final StringPath note = createString("note");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPerson person;

    public final BooleanPath reperibilityManagerApprovalRequired = createBoolean("reperibilityManagerApprovalRequired");

    public final DateTimePath<org.joda.time.LocalDateTime> reperibilityManagerApproved = createDateTime("reperibilityManagerApproved", org.joda.time.LocalDateTime.class);

    public final EnumPath<models.enumerate.ShiftSlot> shiftSlot = createEnum("shiftSlot", models.enumerate.ShiftSlot.class);

    public final DateTimePath<org.joda.time.LocalDateTime> startAt = createDateTime("startAt", org.joda.time.LocalDateTime.class);

    public final models.query.QPerson teamMate;

    public final EnumPath<models.flows.enumerate.CompetenceRequestType> type = createEnum("type", models.flows.enumerate.CompetenceRequestType.class);

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> value = createNumber("value", Integer.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QCompetenceRequest(String variable) {
        this(CompetenceRequest.class, forVariable(variable), INITS);
    }

    public QCompetenceRequest(Path<? extends CompetenceRequest> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCompetenceRequest(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCompetenceRequest(PathMetadata metadata, PathInits inits) {
        this(CompetenceRequest.class, metadata, inits);
    }

    public QCompetenceRequest(Class<? extends CompetenceRequest> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
        this.teamMate = inits.isInitialized("teamMate") ? new models.query.QPerson(forProperty("teamMate"), inits.get("teamMate")) : null;
    }

}
