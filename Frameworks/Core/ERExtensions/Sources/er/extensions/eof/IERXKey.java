package er.extensions.eof;

/**
 * General keypath interface. Should be used by all consumers of keypaths, which only need to use
 * their {@link #key()}s. This allows alternative implementations to {@link ERXKey}.
 *
 * @param <T> the type of the value of this key
 *
 * @author sgaertner
 */
@FunctionalInterface
public interface IERXKey<T> {

	String key();

}
