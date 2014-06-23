package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Option;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QOption is a Querydsl query type for Option
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QOption extends EntityPathBase<Option> {

    private static final long serialVersionUID = -1280047975L;

    public static final QOption option = new QOption("option");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final BooleanPath adjustRange = createBoolean("adjustRange");

    public final BooleanPath adjustRangeDay = createBoolean("adjustRangeDay");

    public final BooleanPath autoRange = createBoolean("autoRange");

    public final DateTimePath<java.util.Date> date = createDateTime("date", java.util.Date.class);

    public final BooleanPath EasterChristmas = createBoolean("EasterChristmas");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath expiredVacationDay = createBoolean("expiredVacationDay");

    public final BooleanPath expiredVacationMonth = createBoolean("expiredVacationMonth");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath otherHeadOffice = createString("otherHeadOffice");

    public final BooleanPath patronDay = createBoolean("patronDay");

    public final BooleanPath patronMonth = createBoolean("patronMonth");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final StringPath recoveryAp = createString("recoveryAp");

    public final BooleanPath recoveryMonth = createBoolean("recoveryMonth");

    public final StringPath tipo_ferie_gen = createString("tipo_ferie_gen");

    public final StringPath tipo_permieg = createString("tipo_permieg");

    public final StringPath vacationType = createString("vacationType");

    public final StringPath vacationTypeP = createString("vacationTypeP");

    public QOption(String variable) {
        super(Option.class, forVariable(variable));
    }

    public QOption(Path<? extends Option> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOption(PathMetadata<?> metadata) {
        super(Option.class, metadata);
    }

}

