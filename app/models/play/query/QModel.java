package models.play.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import play.db.jpa.Model;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QModel is a Querydsl query type for Model
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QModel extends EntityPathBase<Model> {

    private static final long serialVersionUID = 948359744L;

    public static final QModel model = new QModel("model");

    public final QGenericModel _super = new QGenericModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QModel(String variable) {
        super(Model.class, forVariable(variable));
    }

    public QModel(Path<? extends Model> path) {
        super(path.getType(), path.getMetadata());
    }

    public QModel(PathMetadata metadata) {
        super(Model.class, metadata);
    }

}

