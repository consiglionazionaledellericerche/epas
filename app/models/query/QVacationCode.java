package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.VacationCode;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QVacationCode is a Querydsl query type for VacationCode
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QVacationCode extends EntityPathBase<VacationCode> {

    private static final long serialVersionUID = -1221959794L;

    public static final QVacationCode vacationCode = new QVacationCode("vacationCode");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> permissionDays = createNumber("permissionDays", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> vacationDays = createNumber("vacationDays", Integer.class);

    public final ListPath<models.VacationPeriod, QVacationPeriod> vacationPeriod = this.<models.VacationPeriod, QVacationPeriod>createList("vacationPeriod", models.VacationPeriod.class, QVacationPeriod.class, PathInits.DIRECT2);

    public QVacationCode(String variable) {
        super(VacationCode.class, forVariable(variable));
    }

    public QVacationCode(Path<? extends VacationCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVacationCode(PathMetadata<?> metadata) {
        super(VacationCode.class, metadata);
    }

}

