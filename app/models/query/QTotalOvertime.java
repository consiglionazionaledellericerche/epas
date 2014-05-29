package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.TotalOvertime;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QTotalOvertime is a Querydsl query type for TotalOvertime
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QTotalOvertime extends EntityPathBase<TotalOvertime> {

    private static final long serialVersionUID = -695659423;

    public static final QTotalOvertime totalOvertime = new QTotalOvertime("totalOvertime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHours = createNumber("numberOfHours", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QTotalOvertime(String variable) {
        super(TotalOvertime.class, forVariable(variable));
    }

    public QTotalOvertime(Path<? extends TotalOvertime> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTotalOvertime(PathMetadata<?> metadata) {
        super(TotalOvertime.class, metadata);
    }

}

