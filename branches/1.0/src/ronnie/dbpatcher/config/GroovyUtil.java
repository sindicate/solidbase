package ronnie.dbpatcher.config;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.logicacmg.idt.commons.SystemException;

public class GroovyUtil
{
	static public Object evaluate( File file, Map binding )
	{
		try
		{
			return new GroovyShell( new Binding( binding ) ).evaluate( file );
		}
		catch( IOException e )
		{
			throw new SystemException( e );
		}
	}
}
