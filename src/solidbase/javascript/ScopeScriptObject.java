package solidbase.javascript;

import jdk.nashorn.internal.objects.annotations.Attribute;
import jdk.nashorn.internal.objects.annotations.Function;
import jdk.nashorn.internal.runtime.ScriptObject;
import solidstack.script.scopes.Scope;


public class ScopeScriptObject extends ScriptObject
{
	private final Scope scope;

	// key for the forEach invoker callback
	private final static Object FOREACH_INVOKER_KEY = new Object();


	public ScopeScriptObject( final Scope scope )
	{
		this.scope = scope;
	}

	/**
	 * ECMA6 23.1.3.1 Map.prototype.clear ( )
	 *
	 * @param self the self reference
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static void clear( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.3 Map.prototype.delete ( key )
	 *
	 * @param self the self reference
	 * @param key the key to delete
	 * @return true if the key was deleted
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static boolean delete( final Object self, final Object key )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.7 Map.prototype.has ( key )
	 *
	 * @param self the self reference
	 * @param key the key
	 * @return true if key is contained
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static boolean has( final Object self, final Object key )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.9 Map.prototype.set ( key , value )
	 *
	 * @param self the self reference
	 * @param key the key
	 * @param value the value
	 * @return this Map object
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static Object set( final Object self, final Object key, final Object value )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.6 Map.prototype.get ( key )
	 *
	 * @param self the self reference
	 * @param key the key
	 * @return the associated value or undefined
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static Object get( final Object self, final Object key )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.10 get Map.prototype.size
	 *
	 * @param self the self reference
	 * @return the size of the map
	 */
//	@Getter( attributes = Attribute.NOT_ENUMERABLE | Attribute.IS_ACCESSOR, where = Where.PROTOTYPE )
	public static int size( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.4 Map.prototype.entries ( )
	 *
	 * @param self the self reference
	 * @return an iterator over the Map's entries
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static Object entries( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.8 Map.prototype.keys ( )
	 *
	 * @param self the self reference
	 * @return an iterator over the Map's keys
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static Object keys( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.11 Map.prototype.values ( )
	 *
	 * @param self the self reference
	 * @return an iterator over the Map's values
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE )
	public static Object values( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA6 23.1.3.12 Map.prototype [ @@iterator ]( )
	 *
	 * @param self the self reference
	 * @return An iterator over the Map's entries
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE, name = "@@iterator" )
	public static Object getIterator( final Object self )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *
	 * @param self the self reference
	 * @param callbackFn the callback function
	 * @param thisArg optional this-object
	 */
	@Function( attributes = Attribute.NOT_ENUMERABLE, arity = 1 )
	public static void forEach( final Object self, final Object callbackFn, final Object thisArg )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns a canonicalized key object by converting numbers to their narrowest representation and ConsStrings to
	 * strings. Conversion of Double to Integer also takes care of converting -0 to 0 as required by step 6 of ECMA6
	 * 23.1.3.9.
	 *
	 * @param key a key
	 * @return the canonical key
	 */
	static Object convertKey( final Object key )
	{
		throw new UnsupportedOperationException();
	}
}
