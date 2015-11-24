package play.db.jpa.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BeanPath;
import com.mysema.query.types.path.StringPath;
import play.db.jpa.FileAttachment;

import javax.annotation.Generated;

import static com.mysema.query.types.PathMetadataFactory.forVariable;


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

