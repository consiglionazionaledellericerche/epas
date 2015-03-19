package play.db.jpa.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import play.db.jpa.FileAttachment;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QFileAttachment is a Querydsl query type for FileAttachment
 */
@Generated("com.mysema.query.codegen.EmbeddableSerializer")
public class QFileAttachment extends BeanPath<FileAttachment> {

    private static final long serialVersionUID = -1147555192L;

    public static final QFileAttachment fileAttachment = new QFileAttachment("fileAttachment");

    public final StringPath filename = createString("filename");

    public QFileAttachment(String variable) {
        super(FileAttachment.class, forVariable(variable));
    }

    public QFileAttachment(Path<? extends FileAttachment> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFileAttachment(PathMetadata<?> metadata) {
        super(FileAttachment.class, metadata);
    }

}

