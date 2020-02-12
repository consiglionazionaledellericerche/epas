package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.OrganizationShiftTimeTable;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganizationShiftTimeTable is a Querydsl query type for OrganizationShiftTimeTable
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QOrganizationShiftTimeTable extends EntityPathBase<OrganizationShiftTimeTable> {

    private static final long serialVersionUID = -1970111274L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganizationShiftTimeTable organizationShiftTimeTable = new QOrganizationShiftTimeTable("organizationShiftTimeTable");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final EnumPath<models.enumerate.CalculationType> calculationType = createEnum("calculationType", models.enumerate.CalculationType.class);

    public final BooleanPath considerEverySlot = createBoolean("considerEverySlot");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    public final QOffice office;

    public final SetPath<models.OrganizationShiftSlot, QOrganizationShiftSlot> organizationShiftSlot = this.<models.OrganizationShiftSlot, QOrganizationShiftSlot>createSet("organizationShiftSlot", models.OrganizationShiftSlot.class, QOrganizationShiftSlot.class, PathInits.DIRECT2);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.ShiftType, QShiftType> shiftTypes = this.<models.ShiftType, QShiftType>createList("shiftTypes", models.ShiftType.class, QShiftType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QOrganizationShiftTimeTable(String variable) {
        this(OrganizationShiftTimeTable.class, forVariable(variable), INITS);
    }

    public QOrganizationShiftTimeTable(Path<? extends OrganizationShiftTimeTable> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganizationShiftTimeTable(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganizationShiftTimeTable(PathMetadata metadata, PathInits inits) {
        this(OrganizationShiftTimeTable.class, metadata, inits);
    }

    public QOrganizationShiftTimeTable(Class<? extends OrganizationShiftTimeTable> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

