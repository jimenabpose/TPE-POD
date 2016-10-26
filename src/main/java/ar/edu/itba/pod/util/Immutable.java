package ar.edu.itba.pod.util;

import java.lang.annotation.Documented;

/**
 * Marks the class as immutable.
 * 
 * Immutable classes may be shared among threads without synchroniation.
 * Note that the implementation may not be totally immutable. In those cases DO NOT 
 * mutate the state and assume it is not mutable as a contract.
 */
@Documented
public @interface Immutable {

}
