package solidbase.javascript;

import java.util.Collection;
import java.util.Set;

import jdk.nashorn.api.scripting.JSObject;


public class MyJSObject implements JSObject
{
	@Override
	public Object call( Object thiz, Object... args )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object newObject( Object... args )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object eval( String s )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getMember( String name )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getSlot( int index )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasMember( String name )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasSlot( int slot )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeMember( String name )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMember( String name, Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSlot( int index, Object value )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Object> values()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstance( Object instance )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstanceOf( Object clazz )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClassName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isFunction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isStrictFunction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isArray()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double toNumber()
	{
		throw new UnsupportedOperationException();
	}
}
