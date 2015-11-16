package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;
import models.Qualification;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


/**
 * QQualification is a Querydsl query type for Qualification
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QQualification extends EntityPathBase<Qualification> {

    private static final long serialVersionUID = -1883508405L;

    public static final QQualification qualification1 = new QQualification("qualification1");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.AbsenceType, QAbsenceType> absenceTypes = this.<models.AbsenceType, QAbsenceType>createList("absenceTypes", models.AbsenceType.class, QAbsenceType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> person = this.<models.Person, QPerson>createList("person", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final NumberPath<Integer> qualification = createNumber("qualification", Integer.class);

    public QQualification(String variable) {
        super(Qualification.class, forVariable(variable));
    }

    public QQualification(Path<? extends Qualification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQualification(PathMetadata<?> metadata) {
        super(Qualification.class, metadata);
    }

}

