package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.WorkingTimeType;


/**
 * QWorkingTimeType is a Querydsl query type for WorkingTimeType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QWorkingTimeType extends EntityPathBase<WorkingTimeType> {

    private static final long serialVersionUID = 772072020L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWorkingTimeType workingTimeType = new QWorkingTimeType("workingTimeType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createList("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath horizontal = createBoolean("horizontal");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath mealTicketEnabled = createBoolean("mealTicketEnabled");

    public final QOffice office;

    public final NumberPath<Long> periodValueId = createNumber("periodValueId", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonWorkingTimeType, QPersonWorkingTimeType> personWorkingTimeType = this.<models.PersonWorkingTimeType, QPersonWorkingTimeType>createList("personWorkingTimeType", models.PersonWorkingTimeType.class, QPersonWorkingTimeType.class, PathInits.DIRECT2);

    public final BooleanPath shift = createBoolean("shift");

    public final ListPath<models.WorkingTimeTypeDay, QWorkingTimeTypeDay> workingTimeTypeDays = this.<models.WorkingTimeTypeDay, QWorkingTimeTypeDay>createList("workingTimeTypeDays", models.WorkingTimeTypeDay.class, QWorkingTimeTypeDay.class, PathInits.DIRECT2);

    public QWorkingTimeType(String variable) {
        this(WorkingTimeType.class, forVariable(variable), INITS);
    }

    public QWorkingTimeType(Path<? extends WorkingTimeType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QWorkingTimeType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QWorkingTimeType(PathMetadata<?> metadata, PathInits inits) {
        this(WorkingTimeType.class, metadata, inits);
    }

    public QWorkingTimeType(Class<? extends WorkingTimeType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

