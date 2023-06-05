package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.User;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 1339629807L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.BadgeReader, QBadgeReader> badgeReaders = this.<models.BadgeReader, QBadgeReader>createList("badgeReaders", models.BadgeReader.class, QBadgeReader.class, PathInits.DIRECT2);

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DatePath<org.joda.time.LocalDate> expireDate = createDate("expireDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> expireRecoveryToken = createDate("expireRecoveryToken", org.joda.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath keycloakId = createString("keycloakId");

    public final StringPath label = createString("label");

    public final QOffice owner;

    public final StringPath password = createString("password");

    public final StringPath passwordSha512 = createString("passwordSha512");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath recoveryToken = createString("recoveryToken");

    public final SetPath<models.enumerate.AccountRole, EnumPath<models.enumerate.AccountRole>> roles = this.<models.enumerate.AccountRole, EnumPath<models.enumerate.AccountRole>>createSet("roles", models.enumerate.AccountRole.class, EnumPath.class, PathInits.DIRECT2);

    public final StringPath subjectId = createString("subjectId");

    public final BooleanPath systemUser = createBoolean("systemUser");

    public final StringPath username = createString("username");

    public final ListPath<models.UsersRolesOffices, QUsersRolesOffices> usersRolesOffices = this.<models.UsersRolesOffices, QUsersRolesOffices>createList("usersRolesOffices", models.UsersRolesOffices.class, QUsersRolesOffices.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new QOffice(forProperty("owner"), inits.get("owner")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

