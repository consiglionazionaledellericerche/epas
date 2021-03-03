package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonHourForOvertime;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonHourForOvertime is a Querydsl query type for PersonHourForOvertime
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonHourForOvertime extends EntityPathBase<PersonHourForOvertime> {

    private static final long serialVersionUID = -1600219059L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonHourForOvertime personHourForOvertime = new QPersonHourForOvertime("personHourForOvertime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHourForOvertime = createNumber("numberOfHourForOvertime", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonHourForOvertime(String variable) {
        this(PersonHourForOvertime.class, forVariable(variable), INITS);
    }

    public QPersonHourForOvertime(Path<? extends PersonHourForOvertime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonHourForOvertime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonHourForOvertime(PathMetadata metadata, PathInits inits) {
        this(PersonHourForOvertime.class, metadata, inits);
    }

    public QPersonHourForOvertime(Class<? extends PersonHourForOvertime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

