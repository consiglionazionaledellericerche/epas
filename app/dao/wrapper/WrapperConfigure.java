package dao.wrapper;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * @author marco
 *
 */
public class WrapperConfigure extends AbstractModule {

	@Override
	protected void configure() {
		install(new FactoryModuleBuilder()
	     	.implement(IWrapperPerson.class, WrapperPerson.class)
     		.implement(IWrapperContract.class, WrapperContract.class)
     		.implement(IWrapperWorkingTimeType.class, WrapperWorkingTimeType.class)
     		.implement(IWrapperCompetenceCode.class, WrapperCompetenceCode.class)
	     	.build(IWrapperFactory.class));
//		install(new FactoryModuleBuilder()
//     		.build(IWrapperFactory.class));
	}
}
