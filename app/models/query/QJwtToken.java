package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.JwtToken;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QJwtToken is a Querydsl query type for JwtToken
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QJwtToken extends EntityPathBase<JwtToken> {

    private static final long serialVersionUID = 8590486L;

    public static final QJwtToken jwtToken = new QJwtToken("jwtToken");

    public final models.base.query.QMutableModel _super = new models.base.query.QMutableModel(this);

    public final StringPath accessToken = createString("accessToken");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final DateTimePath<org.joda.time.LocalDateTime> expiresAt = createDateTime("expiresAt", org.joda.time.LocalDateTime.class);

    public final NumberPath<Integer> expiresIn = createNumber("expiresIn", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath idToken = createString("idToken");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final StringPath refreshToken = createString("refreshToken");

    public final StringPath scope = createString("scope");

    public final DateTimePath<org.joda.time.LocalDateTime> takenAt = createDateTime("takenAt", org.joda.time.LocalDateTime.class);

    public final StringPath tokenType = createString("tokenType");

    //inherited
    public final DateTimePath<org.joda.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QJwtToken(String variable) {
        super(JwtToken.class, forVariable(variable));
    }

    public QJwtToken(Path<? extends JwtToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QJwtToken(PathMetadata metadata) {
        super(JwtToken.class, metadata);
    }

}

