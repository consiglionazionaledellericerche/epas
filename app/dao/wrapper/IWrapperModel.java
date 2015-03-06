package dao.wrapper;

import models.base.BaseModel;

/**
 * @author marco
 *
 * @param <T>
 */
public interface IWrapperModel<T extends BaseModel> {

	T getValue();

}
