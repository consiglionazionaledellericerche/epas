package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Certification;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCertification is a Querydsl query type for Certification
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final StringPath warnings = createString("warnings");

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public final ComparablePath<org.joda.time.YearMonth> yearMonth = createComparable("yearMonth", org.joda.time.YearMonth.class);

    public QCertification(String variable) {
        this(Certification.class, forVariable(variable), INITS);
    }

    public QCertification(Path<? extends Certification> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCertification(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCertification(PathMetadata metadata, PathInits inits) {
        this(Certification.class, metadata, inits);
    }

    public QCertification(Class<? extends Certification> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

