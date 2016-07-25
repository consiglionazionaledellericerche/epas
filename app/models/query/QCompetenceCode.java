package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.CompetenceCode;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QCompetenceCode is a Querydsl query type for CompetenceCode
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QCompetenceCode extends EntityPathBase<CompetenceCode> {

    private static final long serialVersionUID = 435340666L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCompetenceCode competenceCode = new QCompetenceCode("competenceCode");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath codeToPresence = createString("codeToPresence");

    public final ListPath<models.Competence, QCompetence> competence = this.<models.Competence, QCompetence>createList("competence", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final QCompetenceCodeGroup competenceCodeGroup;

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<models.enumerate.LimitType> limitType = createEnum("limitType", models.enumerate.LimitType.class);

    public final NumberPath<Integer> limitValue = createNumber("limitValue", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public QCompetenceCode(String variable) {
        this(CompetenceCode.class, forVariable(variable), INITS);
    }

    public QCompetenceCode(Path<? extends CompetenceCode> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCompetenceCode(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QCompetenceCode(PathMetadata<?> metadata, PathInits inits) {
        this(CompetenceCode.class, metadata, inits);
    }

    public QCompetenceCode(Class<? extends CompetenceCode> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceCodeGroup = inits.isInitialized("competenceCodeGroup") ? new QCompetenceCodeGroup(forProperty("competenceCodeGroup")) : null;
    }

}

