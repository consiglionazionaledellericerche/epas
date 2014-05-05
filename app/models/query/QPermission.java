package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Permission;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPermission is a Querydsl query type for Permission
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPermission extends EntityPathBase<Permission> {

    private static final long serialVersionUID = -1980985357L;

    public static final QPermission permission = new QPermission("permission");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.Group, QGroup> groups = this.<models.Group, QGroup>createList("groups", models.Group.class, QGroup.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.User, QUser> users = this.<models.User, QUser>createList("users", models.User.class, QUser.class, PathInits.DIRECT2);

    public QPermission(String variable) {
        super(Permission.class, forVariable(variable));
    }

    public QPermission(Path<? extends Permission> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPermission(PathMetadata<?> metadata) {
        super(Permission.class, metadata);
    }

}

