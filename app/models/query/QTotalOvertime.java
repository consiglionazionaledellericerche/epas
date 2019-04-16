package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.TotalOvertime;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTotalOvertime is a Querydsl query type for TotalOvertime
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTotalOvertime extends EntityPathBase<TotalOvertime> {

    private static final long serialVersionUID = -695659423L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTotalOvertime totalOvertime = new QTotalOvertime("totalOvertime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> date = createDate("date", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHours = createNumber("numberOfHours", Integer.class);

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QTotalOvertime(String variable) {
        this(TotalOvertime.class, forVariable(variable), INITS);
    }

    public QTotalOvertime(Path<? extends TotalOvertime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTotalOvertime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTotalOvertime(PathMetadata metadata, PathInits inits) {
        this(TotalOvertime.class, metadata, inits);
    }

    public QTotalOvertime(Class<? extends TotalOvertime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

