package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.JustifiedBehaviour;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QJustifiedBehaviour is a Querydsl query type for JustifiedBehaviour
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QJustifiedBehaviour extends EntityPathBase<JustifiedBehaviour> {

    private static final long serialVersionUID = 1575250450L;

    public static final QJustifiedBehaviour justifiedBehaviour = new QJustifiedBehaviour("justifiedBehaviour");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SetPath<models.absences.AbsenceTypeJustifiedBehaviour, QAbsenceTypeJustifiedBehaviour> absenceTypesJustifiedBehaviours = this.<models.absences.AbsenceTypeJustifiedBehaviour, QAbsenceTypeJustifiedBehaviour>createSet("absenceTypesJustifiedBehaviours", models.absences.AbsenceTypeJustifiedBehaviour.class, QAbsenceTypeJustifiedBehaviour.class, PathInits.DIRECT2);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<JustifiedBehaviour.JustifiedBehaviourName> name = createEnum("name", JustifiedBehaviour.JustifiedBehaviourName.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QJustifiedBehaviour(String variable) {
        super(JustifiedBehaviour.class, forVariable(variable));
    }

    public QJustifiedBehaviour(Path<? extends JustifiedBehaviour> path) {
        super(path.getType(), path.getMetadata());
    }

    public QJustifiedBehaviour(PathMetadata<?> metadata) {
        super(JustifiedBehaviour.class, metadata);
    }

}

