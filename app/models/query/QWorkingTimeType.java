package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.WorkingTimeType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWorkingTimeType is a Querydsl query type for WorkingTimeType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QWorkingTimeType extends EntityPathBase<WorkingTimeType> {

    private static final long serialVersionUID = 772072020L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWorkingTimeType workingTimeType = new QWorkingTimeType("workingTimeType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createList("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    public final BooleanPath enableAdjustmentForQuantity = createBoolean("enableAdjustmentForQuantity");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    public final BooleanPath horizontal = createBoolean("horizontal");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath mealTicketEnabled = createBoolean("mealTicketEnabled");

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath shift = createBoolean("shift");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final ListPath<models.WorkingTimeTypeDay, QWorkingTimeTypeDay> workingTimeTypeDays = this.<models.WorkingTimeTypeDay, QWorkingTimeTypeDay>createList("workingTimeTypeDays", models.WorkingTimeTypeDay.class, QWorkingTimeTypeDay.class, PathInits.DIRECT2);

    public QWorkingTimeType(String variable) {
        this(WorkingTimeType.class, forVariable(variable), INITS);
    }

    public QWorkingTimeType(Path<? extends WorkingTimeType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWorkingTimeType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWorkingTimeType(PathMetadata metadata, PathInits inits) {
        this(WorkingTimeType.class, metadata, inits);
    }

    public QWorkingTimeType(Class<? extends WorkingTimeType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

