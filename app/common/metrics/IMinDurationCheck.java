package common.metrics;

import java.util.function.Predicate;

/**
 * Per la verifica della durata minima delle richieste; il parametro fornito
 * deve essere in nanosecondi.
 *
 * @author marco
 # @see https://github.com/besmartbeopen/play1-base
 */
public interface IMinDurationCheck extends Predicate<Long> {
}
