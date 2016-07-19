package solidbase.javascript;

import java.util.Objects;

import javax.script.Bindings;

import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;
import solidstack.script.scopes.Scope;
import solidstack.script.scopes.UndefinedException;


/**
 * Mirror object that wraps a given Nashorn Script object.
 *
 * @since 1.8u40
 */
@jdk.Exported
public final class ScriptObjectMirror extends ScriptObject
{
//	private static AccessControlContext getContextAccCtxt()
//	{
//		final Permissions perms = new Permissions();
//		perms.add( new RuntimePermission( Context.NASHORN_GET_CONTEXT ) );
//		return new AccessControlContext( new ProtectionDomain[] { new ProtectionDomain( null, perms ) } );
//	}
//
//	private static final AccessControlContext GET_CONTEXT_ACC_CTXT = getContextAccCtxt();

//	private final ScriptObject sobj;
//	private final Global global;
//	private final boolean strict;
//	private final boolean jsonCompatible;

	private Scope scope;


	public ScriptObjectMirror( Scope scope )
	{
		this.scope = scope;
	}

	@Override
	public boolean equals( final Object other )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString()
	{
		throw new UnsupportedOperationException();
	}

	// JSObject methods

	@Override
	public String getClassName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey( final Object key )
	{
		checkKey( key );
		try
		{
			this.scope.get( (String)key );
			return true;
		}
		catch( UndefinedException e )
		{
			return false;
		}
	}

	@Override
	public boolean containsValue( final Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get( final Object key )
	{
		checkKey( key );
		return this.scope.find( (String)key );
	}

	@Override
	public Object put( Object key, Object value, boolean strict )
	{
		checkKey( key );
		Object old = this.scope.find( (String)key );
		this.scope.set( (String)key, value );
		return old;
	}

	@Override
	public boolean isEmpty()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete a property from this object.
	 *
	 * @param key the property to be deleted
	 *
	 * @return if the delete was successful or not
	 */
	public boolean delete( final Object key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int size()
	{
		throw new UnsupportedOperationException();
	}

	// Support for ECMAScript Object API on mirrors

	/**
	 * Set the __proto__ of this object.
	 *
	 * @param proto new proto for this object
	 */
	public void setProto( final Object proto )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Flag this script object as non extensible
	 *
	 * @return the object after being made non extensible
	 */
	@Override
	public ScriptObjectMirror preventExtensions()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Check if this script object is extensible
	 *
	 * @return true if extensible
	 */
	@Override
	public boolean isExtensible()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMAScript 15.2.3.8 - seal implementation
	 *
	 * @return the sealed script object
	 */
	@Override
	public ScriptObjectMirror seal()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Check whether this script object is sealed
	 *
	 * @return true if sealed
	 */
	@Override
	public boolean isSealed()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * ECMA 15.2.39 - freeze implementation. Freeze this script object
	 *
	 * @return the frozen script object
	 */
	@Override
	public ScriptObjectMirror freeze()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Check whether this script object is frozen
	 *
	 * @return true if frozen
	 */
	@Override
	public boolean isFrozen()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Utility to check if given object is ECMAScript undefined value
	 *
	 * @param obj object to check
	 * @return true if 'obj' is ECMAScript undefined value
	 */
	public static boolean isUndefined( final Object obj )
	{
		return obj == ScriptRuntime.UNDEFINED;
	}

	/**
	 * Utility to convert this script object to the given type.
	 *
	 * @param <T> destination type to convert to
	 * @param type destination type to convert to
	 * @return converted object
	 */
	public <T> T to( final Class<T> type )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Unwrap a script object mirror if needed.
	 *
	 * @param obj object to be unwrapped
	 * @param homeGlobal global to which this object belongs
	 * @return unwrapped object
	 */
	public static Object unwrap( final Object obj, final Object homeGlobal )
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Unwrap an array of script object mirrors if needed.
	 *
	 * @param args array to be unwrapped
	 * @param homeGlobal global to which this object belongs
	 * @return unwrapped array
	 */
	public static Object[] unwrapArray( final Object[] args, final Object homeGlobal )
	{
		if( args == null || args.length == 0 )
			return args;

		final Object[] newArgs = new Object[ args.length ];
		int index = 0;
		for( final Object obj : args )
		{
			newArgs[ index ] = unwrap( obj, homeGlobal );
			index++;
		}
		return newArgs;
	}

	/**
	 * Are the given objects mirrors to same underlying object?
	 *
	 * @param obj1 first object
	 * @param obj2 second object
	 * @return true if obj1 and obj2 are identical script objects or mirrors of it.
	 */
	public static boolean identical( final Object obj1, final Object obj2 )
	{
		throw new UnsupportedOperationException();
	}

	// package-privates below this.

	static Object translateUndefined( final Object obj )
	{
		return obj == ScriptRuntime.UNDEFINED ? null : obj;
	}

	/**
	 * Ensures the key is not null, empty string, or a non-String object. The contract of the {@link Bindings} interface
	 * requires that these are not accepted as keys.
	 *
	 * @param key the key to check
	 * @throws NullPointerException if key is null
	 * @throws ClassCastException if key is not a String
	 * @throws IllegalArgumentException if key is empty string
	 */
	private static void checkKey( final Object key )
	{
		Objects.requireNonNull( key, "key can not be null" );

		if( !( key instanceof String ) )
			throw new ClassCastException( "key should be a String. It is " + key.getClass().getName() + " instead." );
		else if( ( (String)key ).length() == 0 )
			throw new IllegalArgumentException( "key can not be empty" );
	}

	@Override
	public Object getDefaultValue( final Class<?> hint )
	{
		throw new UnsupportedOperationException();
	}
}
