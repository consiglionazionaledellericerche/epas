package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.YearRecap;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QYearRecap is a Querydsl query type for YearRecap
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QYearRecap extends EntityPathBase<YearRecap> {

    private static final long serialVersionUID = -1232312354L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QYearRecap yearRecap = new QYearRecap("yearRecap");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.sql.Timestamp> lastModified = createDateTime("lastModified", java.sql.Timestamp.class);

    public final NumberPath<Integer> overtime = createNumber("overtime", Integer.class);

    public final NumberPath<Integer> overtimeAp = createNumber("overtimeAp", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final NumberPath<Integer> recg = createNumber("recg", Integer.class);

    public final NumberPath<Integer> recgap = createNumber("recgap", Integer.class);

    public final NumberPath<Integer> recguap = createNumber("recguap", Integer.class);

    public final NumberPath<Integer> recm = createNumber("recm", Integer.class);

    public final NumberPath<Integer> remaining = createNumber("remaining", Integer.class);

    public final NumberPath<Integer> remainingAp = createNumber("remainingAp", Integer.class);

    public final NumberPath<Short> year = createNumber("year", Short.class);

    public QYearRecap(String variable) {
        this(YearRecap.class, forVariable(variable), INITS);
    }

    public QYearRecap(Path<? extends YearRecap> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QYearRecap(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QYearRecap(PathMetadata<?> metadata, PathInits inits) {
        this(YearRecap.class, metadata, inits);
    }

    public QYearRecap(Class<? extends YearRecap> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

