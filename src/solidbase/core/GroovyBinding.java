package solidbase.core;

import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.MetaClass;
import solidstack.script.scopes.Scope;


public class GroovyBinding extends Binding
{
	private Scope scope;


	public GroovyBinding( Scope scope )
	{
		this.scope = scope;
	}

	@Override
	public Object getProperty( String arg0 )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getVariable( String name )
	{
		System.out.println( "getVariable: " + name );
		return this.scope.find( name );
	}

	@Override
	public Map getVariables()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasVariable( String name )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setProperty( String arg0, Object arg1 )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVariable( String name, Object value )
	{
		System.out.println( "setVariable: " + name );
		this.scope.setOrVar( name, value );
	}

	@Override
	public MetaClass getMetaClass()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object invokeMethod( String name, Object args )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMetaClass( MetaClass metaClass )
	{
		throw new UnsupportedOperationException();
	}
}
