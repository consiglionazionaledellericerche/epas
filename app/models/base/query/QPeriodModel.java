package models.base.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.base.PeriodModel;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QPeriodModel is a Querydsl query type for PeriodModel
 */
@Generated("com.querydsl.codegen.SupertypeSerializer")
public class QPeriodModel extends EntityPathBase<PeriodModel> {

    private static final long serialVersionUID = 1259083055L;

    public static final QPeriodModel periodModel = new QPeriodModel("periodModel");

    public final QBaseModel _super = new QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> beginDate = createDate("beginDate", org.joda.time.LocalDate.class);

    public final DatePath<org.joda.time.LocalDate> endDate = createDate("endDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPeriodModel(String variable) {
        super(PeriodModel.class, forVariable(variable));
    }

    public QPeriodModel(Path<? extends PeriodModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPeriodModel(PathMetadata metadata) {
        super(PeriodModel.class, metadata);
    }

}

