package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.PersonChildren;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QPersonChildren is a Querydsl query type for PersonChildren
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonChildren extends EntityPathBase<PersonChildren> {

    private static final long serialVersionUID = 954421400L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonChildren personChildren = new QPersonChildren("personChildren");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> bornDate = createDate("bornDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath surname = createString("surname");

    public QPersonChildren(String variable) {
        this(PersonChildren.class, forVariable(variable), INITS);
    }

    public QPersonChildren(Path<? extends PersonChildren> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonChildren(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonChildren(PathMetadata<?> metadata, PathInits inits) {
        this(PersonChildren.class, metadata, inits);
    }

    public QPersonChildren(Class<? extends PersonChildren> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

