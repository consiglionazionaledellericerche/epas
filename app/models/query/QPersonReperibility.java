package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;
import models.PersonReperibility;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QPersonReperibility is a Querydsl query type for PersonReperibility
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonReperibility extends EntityPathBase<PersonReperibility> {

    private static final long serialVersionUID = -1594033151L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonReperibility personReperibility = new QPersonReperibility("personReperibility");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath note = createString("note");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final ListPath<models.PersonReperibilityDay, QPersonReperibilityDay> personReperibilityDays = this.<models.PersonReperibilityDay, QPersonReperibilityDay>createList("personReperibilityDays", models.PersonReperibilityDay.class, QPersonReperibilityDay.class, PathInits.DIRECT2);

    public final QPersonReperibilityType personReperibilityType;

    public final DatePath<org.joda.time.LocalDate> startDate = createDate("startDate", org.joda.time.LocalDate.class);

    public QPersonReperibility(String variable) {
        this(PersonReperibility.class, forVariable(variable), INITS);
    }

    public QPersonReperibility(Path<? extends PersonReperibility> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonReperibility(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonReperibility(PathMetadata<?> metadata, PathInits inits) {
        this(PersonReperibility.class, metadata, inits);
    }

    public QPersonReperibility(Class<? extends PersonReperibility> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
        this.personReperibilityType = inits.isInitialized("personReperibilityType") ? new QPersonReperibilityType(forProperty("personReperibilityType"), inits.get("personReperibilityType")) : null;
    }

}

