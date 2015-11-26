package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonReperibilityType;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonReperibilityType is a Querydsl query type for PersonReperibilityType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonReperibilityType extends EntityPathBase<PersonReperibilityType> {

    private static final long serialVersionUID = -1571481893L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonReperibilityType personReperibilityType = new QPersonReperibilityType("personReperibilityType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonReperibility, QPersonReperibility> personReperibilities = this.<models.PersonReperibility, QPersonReperibility>createList("personReperibilities", models.PersonReperibility.class, QPersonReperibility.class, PathInits.DIRECT2);

    public final QPerson supervisor;

    public QPersonReperibilityType(String variable) {
        this(PersonReperibilityType.class, forVariable(variable), INITS);
    }

    public QPersonReperibilityType(Path<? extends PersonReperibilityType> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonReperibilityType(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonReperibilityType(PathMetadata<?> metadata, PathInits inits) {
        this(PersonReperibilityType.class, metadata, inits);
    }

    public QPersonReperibilityType(Class<? extends PersonReperibilityType> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.supervisor = inits.isInitialized("supervisor") ? new QPerson(forProperty("supervisor"), inits.get("supervisor")) : null;
    }

}

