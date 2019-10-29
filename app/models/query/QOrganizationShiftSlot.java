package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.OrganizationShiftSlot;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganizationShiftSlot is a Querydsl query type for OrganizationShiftSlot
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QOrganizationShiftSlot extends EntityPathBase<OrganizationShiftSlot> {

    private static final long serialVersionUID = 1547324681L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganizationShiftSlot organizationShiftSlot = new QOrganizationShiftSlot("organizationShiftSlot");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final TimePath<org.joda.time.LocalTime> beginMealSlot = createTime("beginMealSlot", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> beginSlot = createTime("beginSlot", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endMealSlot = createTime("endMealSlot", org.joda.time.LocalTime.class);

    public final TimePath<org.joda.time.LocalTime> endSlot = createTime("endSlot", org.joda.time.LocalTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> minutesPaid = createNumber("minutesPaid", Integer.class);

    public final NumberPath<Integer> minutesSlot = createNumber("minutesSlot", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QOrganizationShiftTimeTable shiftTimeTable;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QOrganizationShiftSlot(String variable) {
        this(OrganizationShiftSlot.class, forVariable(variable), INITS);
    }

    public QOrganizationShiftSlot(Path<? extends OrganizationShiftSlot> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganizationShiftSlot(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganizationShiftSlot(PathMetadata metadata, PathInits inits) {
        this(OrganizationShiftSlot.class, metadata, inits);
    }

    public QOrganizationShiftSlot(Class<? extends OrganizationShiftSlot> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.shiftTimeTable = inits.isInitialized("shiftTimeTable") ? new QOrganizationShiftTimeTable(forProperty("shiftTimeTable"), inits.get("shiftTimeTable")) : null;
    }

}

