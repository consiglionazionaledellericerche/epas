package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.TimeSlot;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTimeSlot is a Querydsl query type for TimeSlot
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTimeSlot extends EntityPathBase<TimeSlot> {

    private static final long serialVersionUID = 531137935L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTimeSlot timeSlot = new QTimeSlot("timeSlot");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final TimePath<org.joda.time.LocalTime> beginSlot = createTime("beginSlot", org.joda.time.LocalTime.class);

    public final ListPath<models.ContractMandatoryTimeSlot, QContractMandatoryTimeSlot> contractMandatoryTimeSlots = this.<models.ContractMandatoryTimeSlot, QContractMandatoryTimeSlot>createList("contractMandatoryTimeSlots", models.ContractMandatoryTimeSlot.class, QContractMandatoryTimeSlot.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    public final TimePath<org.joda.time.LocalTime> endSlot = createTime("endSlot", org.joda.time.LocalTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QTimeSlot(String variable) {
        this(TimeSlot.class, forVariable(variable), INITS);
    }

    public QTimeSlot(Path<? extends TimeSlot> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTimeSlot(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTimeSlot(PathMetadata metadata, PathInits inits) {
        this(TimeSlot.class, metadata, inits);
    }

    public QTimeSlot(Class<? extends TimeSlot> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

