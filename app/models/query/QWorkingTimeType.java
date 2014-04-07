package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.WorkingTimeType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QWorkingTimeType is a Querydsl query type for WorkingTimeType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QWorkingTimeType extends EntityPathBase<WorkingTimeType> {

    private static final long serialVersionUID = 772072020L;

    public static final QWorkingTimeType workingTimeType = new QWorkingTimeType("workingTimeType");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final ListPath<models.ContractWorkingTimeType, QContractWorkingTimeType> contractWorkingTimeType = this.<models.ContractWorkingTimeType, QContractWorkingTimeType>createList("contractWorkingTimeType", models.ContractWorkingTimeType.class, QContractWorkingTimeType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath mealTicketEnabled = createBoolean("mealTicketEnabled");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonWorkingTimeType, QPersonWorkingTimeType> personWorkingTimeType = this.<models.PersonWorkingTimeType, QPersonWorkingTimeType>createList("personWorkingTimeType", models.PersonWorkingTimeType.class, QPersonWorkingTimeType.class, PathInits.DIRECT2);

    public final BooleanPath shift = createBoolean("shift");

    public final ListPath<models.WorkingTimeTypeDay, QWorkingTimeTypeDay> workingTimeTypeDays = this.<models.WorkingTimeTypeDay, QWorkingTimeTypeDay>createList("workingTimeTypeDays", models.WorkingTimeTypeDay.class, QWorkingTimeTypeDay.class, PathInits.DIRECT2);

    public QWorkingTimeType(String variable) {
        super(WorkingTimeType.class, forVariable(variable));
    }

    public QWorkingTimeType(Path<? extends WorkingTimeType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWorkingTimeType(PathMetadata<?> metadata) {
        super(WorkingTimeType.class, metadata);
    }

}

