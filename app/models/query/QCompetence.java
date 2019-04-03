package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Competence;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCompetence is a Querydsl query type for Competence
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCompetence extends EntityPathBase<Competence> {

    private static final long serialVersionUID = 2103402989L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCompetence competence = new QCompetence("competence");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QCompetenceCode competenceCode;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Integer> exceededMins = createNumber("exceededMins", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath reason = createString("reason");

    public final NumberPath<Integer> valueApproved = createNumber("valueApproved", Integer.class);

    public final NumberPath<java.math.BigDecimal> valueRequested = createNumber("valueRequested", java.math.BigDecimal.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QCompetence(String variable) {
        this(Competence.class, forVariable(variable), INITS);
    }

    public QCompetence(Path<? extends Competence> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCompetence(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCompetence(PathMetadata metadata, PathInits inits) {
        this(Competence.class, metadata, inits);
    }

    public QCompetence(Class<? extends Competence> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceCode = inits.isInitialized("competenceCode") ? new QCompetenceCode(forProperty("competenceCode"), inits.get("competenceCode")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

