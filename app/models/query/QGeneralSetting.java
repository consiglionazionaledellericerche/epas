package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.GeneralSetting;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;


/**
 * QGeneralSetting is a Querydsl query type for GeneralSetting
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QGeneralSetting extends EntityPathBase<GeneralSetting> {

    private static final long serialVersionUID = -1610391444L;

    public static final QGeneralSetting generalSetting = new QGeneralSetting("generalSetting");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath syncBadgesEnabled = createBoolean("syncBadgesEnabled");

    public final BooleanPath syncOfficesEnabled = createBoolean("syncOfficesEnabled");

    public final BooleanPath syncPersonsEnabled = createBoolean("syncPersonsEnabled");

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QGeneralSetting(String variable) {
        super(GeneralSetting.class, forVariable(variable));
    }

    public QGeneralSetting(Path<? extends GeneralSetting> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGeneralSetting(PathMetadata metadata) {
        super(GeneralSetting.class, metadata);
    }

}

