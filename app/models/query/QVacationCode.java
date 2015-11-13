package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;
import models.VacationCode;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


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

