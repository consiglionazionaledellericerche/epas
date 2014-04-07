package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ConfYear;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QConfYear is a Querydsl query type for ConfYear
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QConfYear extends EntityPathBase<ConfYear> {

    private static final long serialVersionUID = 2027912357L;

    public static final QConfYear confYear = new QConfYear("confYear");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final NumberPath<Integer> dayExpiryVacationPastYear = createNumber("dayExpiryVacationPastYear", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> hourMaxToCalculateWorkTime = createNumber("hourMaxToCalculateWorkTime", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> maxRecoveryDaysFourNine = createNumber("maxRecoveryDaysFourNine", Integer.class);

    public final NumberPath<Integer> maxRecoveryDaysOneThree = createNumber("maxRecoveryDaysOneThree", Integer.class);

    public final NumberPath<Integer> monthExpireRecoveryDaysFourNine = createNumber("monthExpireRecoveryDaysFourNine", Integer.class);

    public final NumberPath<Integer> monthExpireRecoveryDaysOneThree = createNumber("monthExpireRecoveryDaysOneThree", Integer.class);

    public final NumberPath<Integer> monthExpiryVacationPastYear = createNumber("monthExpiryVacationPastYear", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QConfYear(String variable) {
        super(ConfYear.class, forVariable(variable));
    }

    public QConfYear(Path<? extends ConfYear> path) {
        super(path.getType(), path.getMetadata());
    }

    public QConfYear(PathMetadata<?> metadata) {
        super(ConfYear.class, metadata);
    }

}

