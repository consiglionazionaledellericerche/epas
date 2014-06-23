package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ValuableCompetence;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QValuableCompetence is a Querydsl query type for ValuableCompetence
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QValuableCompetence extends EntityPathBase<ValuableCompetence> {

    private static final long serialVersionUID = -298743461L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QValuableCompetence valuableCompetence = new QValuableCompetence("valuableCompetence");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath codicecomp = createString("codicecomp");

    public final StringPath descrizione = createString("descrizione");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public QValuableCompetence(String variable) {
        this(ValuableCompetence.class, forVariable(variable), INITS);
    }

    public QValuableCompetence(Path<? extends ValuableCompetence> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QValuableCompetence(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QValuableCompetence(PathMetadata<?> metadata, PathInits inits) {
        this(ValuableCompetence.class, metadata, inits);
    }

    public QValuableCompetence(Class<? extends ValuableCompetence> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

