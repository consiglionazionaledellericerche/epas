package common.metrics;

import java.util.function.Predicate;

/**
 * Per la verifica della durata minima delle richieste; il parametro fornito
 * deve essere in nanosecondi.
 *
 * @author marco
 *
 */
public interface IMinDurationCheck extends Predicate<Long> {
}
