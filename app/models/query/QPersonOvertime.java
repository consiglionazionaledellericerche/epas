package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonOvertime;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonOvertime is a Querydsl query type for PersonOvertime
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonOvertime extends EntityPathBase<PersonOvertime> {

    private static final long serialVersionUID = -175048646L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonOvertime personOvertime = new QPersonOvertime("personOvertime");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> dateOfUpdate = createDate("dateOfUpdate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> numberOfHours = createNumber("numberOfHours", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QPersonOvertime(String variable) {
        this(PersonOvertime.class, forVariable(variable), INITS);
    }

    public QPersonOvertime(Path<? extends PersonOvertime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonOvertime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonOvertime(PathMetadata metadata, PathInits inits) {
        this(PersonOvertime.class, metadata, inits);
    }

    public QPersonOvertime(Class<? extends PersonOvertime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

