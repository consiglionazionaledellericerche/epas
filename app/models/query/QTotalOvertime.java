package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;

import models.TotalOvertime;


/**
 * QTotalOvertime is a Querydsl query type for TotalOvertime
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
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

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QTotalOvertime(String variable) {
        this(TotalOvertime.class, forVariable(variable), INITS);
    }

    public QTotalOvertime(Path<? extends TotalOvertime> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QTotalOvertime(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QTotalOvertime(PathMetadata<?> metadata, PathInits inits) {
        this(TotalOvertime.class, metadata, inits);
    }

    public QTotalOvertime(Class<? extends TotalOvertime> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
    }

}

