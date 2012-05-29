import models.Permission;
import models.Person;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;


/**
 * @author cristian
 *
 */
@OnApplicationStart
public class Bootstrap extends Job {
	

//	public void doJob() {
//		if (Permission.count() == 0) {
//			Fixtures.delete();
//			Fixtures.loadModels("permission.yml");
//		} 
//	}

}
