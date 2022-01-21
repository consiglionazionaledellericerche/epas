package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonalWorkingTime;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonalWorkingTime is a Querydsl query type for PersonalWorkingTime
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonalWorkingTime extends EntityPathBase<PersonalWorkingTime> {

    private static final long serialVersionUID = -117806694L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonalWorkingTime personalWorkingTime = new QPersonalWorkingTime("personalWorkingTime");

    public final models.base.query.QPropertyInPeriod _super = new models.base.query.QPropertyInPeriod(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final QContract contract;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QTimeSlot timeSlot;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonalWorkingTime(String variable) {
        this(PersonalWorkingTime.class, forVariable(variable), INITS);
    }

    public QPersonalWorkingTime(Path<? extends PersonalWorkingTime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonalWorkingTime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonalWorkingTime(PathMetadata metadata, PathInits inits) {
        this(PersonalWorkingTime.class, metadata, inits);
    }

    public QPersonalWorkingTime(Class<? extends PersonalWorkingTime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.timeSlot = inits.isInitialized("timeSlot") ? new QTimeSlot(forProperty("timeSlot"), inits.get("timeSlot")) : null;
    }

}

