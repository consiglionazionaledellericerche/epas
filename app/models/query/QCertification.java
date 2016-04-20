package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Certification;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCertification is a Querydsl query type for Certification
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCertification extends EntityPathBase<Certification> {

    private static final long serialVersionUID = -1896699882L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCertification certification = new QCertification("certification");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> attestatiId = createNumber("attestatiId", Integer.class);

    public final EnumPath<models.enumerate.CertificationType> certificationType = createEnum("certificationType", models.enumerate.CertificationType.class);

    public final StringPath content = createString("content");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath problems = createString("problems");

    public final StringPath warnings = createString("warnings");

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QCertification(String variable) {
        this(Certification.class, forVariable(variable), INITS);
    }

    public QCertification(Path<? extends Certification> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCertification(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCertification(PathMetadata<?> metadata, PathInits inits) {
        this(Certification.class, metadata, inits);
    }

    public QCertification(Class<? extends Certification> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

