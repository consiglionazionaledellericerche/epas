package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.UsersPermissionsOffices;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QUsersPermissionsOffices is a Querydsl query type for UsersPermissionsOffices
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QUsersPermissionsOffices extends EntityPathBase<UsersPermissionsOffices> {

    private static final long serialVersionUID = 2108590231L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUsersPermissionsOffices usersPermissionsOffices = new QUsersPermissionsOffices("usersPermissionsOffices");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    public final QPermission permission;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QUser user;

    public QUsersPermissionsOffices(String variable) {
        this(UsersPermissionsOffices.class, forVariable(variable), INITS);
    }

    public QUsersPermissionsOffices(Path<? extends UsersPermissionsOffices> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUsersPermissionsOffices(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUsersPermissionsOffices(PathMetadata<?> metadata, PathInits inits) {
        this(UsersPermissionsOffices.class, metadata, inits);
    }

    public QUsersPermissionsOffices(Class<? extends UsersPermissionsOffices> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office")) : null;
        this.permission = inits.isInitialized("permission") ? new QPermission(forProperty("permission")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

