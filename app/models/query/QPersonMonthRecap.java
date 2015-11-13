package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import models.PersonMonthRecap;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QPersonMonthRecap is a Querydsl query type for PersonMonthRecap
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonMonthRecap extends EntityPathBase<PersonMonthRecap> {

    private static final long serialVersionUID = -642314632L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonMonthRecap personMonthRecap = new QPersonMonthRecap("personMonthRecap");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> fromDate = createDate("fromDate", org.joda.time.LocalDate.class);

    public final BooleanPath hoursApproved = createBoolean("hoursApproved");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final DatePath<org.joda.time.LocalDate> toDate = createDate("toDate", org.joda.time.LocalDate.class);

    public final NumberPath<Integer> trainingHours = createNumber("trainingHours", Integer.class);

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QPersonMonthRecap(String variable) {
        this(PersonMonthRecap.class, forVariable(variable), INITS);
    }

    public QPersonMonthRecap(Path<? extends PersonMonthRecap> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonMonthRecap(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonMonthRecap(PathMetadata<?> metadata, PathInits inits) {
        this(PersonMonthRecap.class, metadata, inits);
    }

    public QPersonMonthRecap(Class<? extends PersonMonthRecap> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

