package solidbase.javascript;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jdk.internal.dynalink.CallSiteDescriptor;
import jdk.internal.dynalink.linker.GuardedInvocation;
import jdk.internal.dynalink.linker.LinkRequest;
import jdk.nashorn.internal.runtime.AccessorProperty;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.FindProperty;
import jdk.nashorn.internal.runtime.Property;
import jdk.nashorn.internal.runtime.PropertyMap;
import jdk.nashorn.internal.runtime.ScriptObject;


public class MyScriptObject extends ScriptObject
{
	@Override
	protected boolean isGlobal()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addBoundProperties( ScriptObject source )
	{
		System.out.println( "addBoundProperties" );
		for( Property property : source.getMap().getProperties() )
			System.out.println( "    " + property );
	}

	@Override
	public void addBoundProperties( ScriptObject source, Property[] properties )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected PropertyMap addBoundProperty( PropertyMap propMap, ScriptObject source, Property property,
			boolean extensible )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addBoundProperties( Object source, AccessorProperty[] properties )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<String> propertyIterator()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Object> valueIterator()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getOwnPropertyDescriptor( String key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getPropertyDescriptor( String key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void invalidateGlobalConstant( String key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean defineOwnProperty( String key, Object propertyDesc, boolean reject )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void defineOwnProperty( int index, Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected FindProperty findProperty( String key, boolean deep, ScriptObject start )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected MethodHandle getCallMethodHandle( FindProperty find, MethodType type, String bindName )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getArgument( int key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setArgument( int key, Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Context getContext()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInitialProto( ScriptObject initialProto )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] getOwnKeys( boolean all, Set<String> nonEnumerable )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasArrayEntries()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getLength()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String safeToString()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getDefaultValue( Class<?> typeHint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstance( ScriptObject instance )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptObject preventExtensions()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLengthNotWritable()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setIsLengthNotWritable()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isExtensible()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptObject seal()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSealed()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptObject freeze()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isFrozen()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isScope()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear( boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey( Object key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue( Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<Object, Object>> entrySet()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Object> keySet()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object put( Object key, Object value, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll( Map<?, ?> otherMap, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove( Object key, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int size()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Object> values()
	{
		throw new UnsupportedOperationException();
	}

//	@Override
//	public GuardedInvocation lookup( CallSiteDescriptor desc, LinkRequest request )
//	{
//		throw new UnsupportedOperationException();
//	}

	@Override
	protected GuardedInvocation findNewMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findCallMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findCallMethodMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findGetMethod( CallSiteDescriptor desc, LinkRequest request, String operator )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findGetIndexMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected MethodHandle findGetIndexMethodHandle( Class<?> returnType, String name, Class<?> elementType,
			CallSiteDescriptor desc )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findSetMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		System.out.println( "findSetMethod" );
		System.out.println( "    desc: " + desc );
		System.out.println( "    request: " + request );
		throw new UnsupportedOperationException();
	}

	@Override
	protected GuardedInvocation findSetIndexMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public GuardedInvocation noSuchMethod( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public GuardedInvocation noSuchProperty( CallSiteDescriptor desc, LinkRequest request )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object invokeNoSuchProperty( String name, boolean isScope, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt( Object key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt( double key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt( int key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble( Object key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble( double key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDouble( int key, int programPoint )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get( Object key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get( double key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get( int key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( Object key, int value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( Object key, double value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( Object key, Object value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( double key, int value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( double key, double value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( double key, Object value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( int key, int value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( int key, double value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void set( int key, Object value, int callSiteFlags )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean has( Object key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean has( double key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean has( int key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasOwnProperty( Object key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasOwnProperty( int key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasOwnProperty( double key )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete( int key, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete( double key, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete( Object key, boolean strict )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected ScriptObject clone() throws CloneNotSupportedException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean useDualFields()
	{
		throw new UnsupportedOperationException();
	}
}
