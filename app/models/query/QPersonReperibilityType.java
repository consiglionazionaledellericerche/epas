package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import models.PersonReperibilityType;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;


/**
 * QPersonReperibilityType is a Querydsl query type for PersonReperibilityType
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonReperibilityType extends EntityPathBase<PersonReperibilityType> {

    private static final long serialVersionUID = -1571481893L;

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

    public QPersonReperibilityType(String variable) {
        super(PersonReperibilityType.class, forVariable(variable));
    }

    public QPersonReperibilityType(Path<? extends PersonReperibilityType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPersonReperibilityType(PathMetadata<?> metadata) {
        super(PersonReperibilityType.class, metadata);
    }

}

