package dao;

import models.base.BaseModel;
import models.base.Revision;

import org.hibernate.envers.RevisionType;

import com.google.common.base.Function;

/**
 * @author marco
 *
 */
public class HistoryValue<T extends BaseModel> {
	
	public final T value;
	public final Revision revision;
	public final RevisionType type;
	
	HistoryValue(T value, Revision revision, RevisionType type) {
		this.value = value;
		this.revision = revision;
		this.type = type;
	}
	
	public static <T extends BaseModel> Function<Object[], HistoryValue<T>> 
		fromTuple(final Class<T> cls) {
		
		return new Function<Object[], HistoryValue<T>>() {
			@Override
			public HistoryValue<T> apply(Object[] tuple) { 
				return new HistoryValue<T>(cls.cast(tuple[0]), (Revision) tuple[1],
						(RevisionType) tuple[2]);
			}
		};
	}
}