package solidbase.core.script;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import solidbase.core.FatalException;

public class ScriptManager
{
	static private final ScriptEngineManager manager = new ScriptEngineManager();

	static
	{
//		System.out.println( "ScriptEngines:" );
//		for( ScriptEngineFactory factory : manager.getEngineFactories() ) {
//			System.out.println( "    EngineName: " + factory.getEngineName() );
//			System.out.println( "    LanguageName: " + factory.getLanguageName() );
//			System.out.println( "    Extensions:" );
//			for( String s : factory.getExtensions() ) System.out.println( "        " + s );
//			System.out.println( "    MimeTypes:" );
//			for( String s : factory.getMimeTypes() ) System.out.println( "        " + s );
//			System.out.println( "    Names:" );
//			for( String s : factory.getNames() ) System.out.println( "        " + s );
//		}
	}

	static public ScriptEngine getEngine( String shortName )
	{
		ScriptEngine result = manager.getEngineByName( shortName );
		if( result == null )
			throw new FatalException( "Could not find script engine: " + shortName );
		return result;
	}
}
