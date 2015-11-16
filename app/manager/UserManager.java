package manager;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import org.joda.time.LocalDate;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by daniele on 13/10/15.
 */
@Slf4j
public class UserManager {

    public void generateRecoveryToken(Person person){

        Preconditions.checkState(person != null && person.isPersistent());

        //generate random token
        SecureRandom random = new SecureRandom();
        String token = new BigInteger(130, random).toString(32);

        //Person person = contactData.person;
        person.user.recoveryToken = token;
        person.user.expireRecoveryToken = LocalDate.now();
        person.user.save();
    }

}
