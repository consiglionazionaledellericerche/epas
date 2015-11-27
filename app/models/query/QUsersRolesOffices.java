package models.query;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;

import models.UsersRolesOffices;


/**
 * QUsersRolesOffices is a Querydsl query type for UsersRolesOffices
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QUsersRolesOffices extends EntityPathBase<UsersRolesOffices> {

    private static final long serialVersionUID = -100507842L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUsersRolesOffices usersRolesOffices = new QUsersRolesOffices("usersRolesOffices");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOffice office;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QRole role;

    public final QUser user;

    public QUsersRolesOffices(String variable) {
        this(UsersRolesOffices.class, forVariable(variable), INITS);
    }

    public QUsersRolesOffices(Path<? extends UsersRolesOffices> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUsersRolesOffices(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUsersRolesOffices(PathMetadata<?> metadata, PathInits inits) {
        this(UsersRolesOffices.class, metadata, inits);
    }

    public QUsersRolesOffices(Class<? extends UsersRolesOffices> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.office = inits.isInitialized("office") ? new QOffice(forProperty("office"), inits.get("office")) : null;
        this.role = inits.isInitialized("role") ? new QRole(forProperty("role")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

