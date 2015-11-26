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

    public static final QCompetenceCode competenceCode = new QCompetenceCode("competenceCode");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath codeToPresence = createString("codeToPresence");

    public final ListPath<models.Competence, QCompetence> competence = this.<models.Competence, QCompetence>createList("competence", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public QCompetenceCode(String variable) {
        super(CompetenceCode.class, forVariable(variable));
    }

    public QCompetenceCode(Path<? extends CompetenceCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCompetenceCode(PathMetadata<?> metadata) {
        super(CompetenceCode.class, metadata);
    }

}

