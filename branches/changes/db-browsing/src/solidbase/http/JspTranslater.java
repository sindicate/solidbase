package solidbase.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;
import java.util.regex.Pattern;

import solidbase.util.Assert;

public class JspTranslater
{
	static final protected Pattern pathPattern = Pattern.compile( "/*(?:(.+?)/+)?([^\\/]+)" );

	static public void main( String... args )
	{
		try
		{
			translatePages( new File( "src" ), new String[] { "solidbase/http/hyperdb/Template.jsp" }, new File( "target/pages" ) );
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}

	static public void translatePages( File baseDir, String[] pages, File outputDir ) throws IOException
	{
		if( pages.length == 0 )
			System.out.println( "No pages to compile" );

		int count = 0;
		for( String page : pages )
		{
			// TODO Directly to outputstream

			File file = new File( page );
			File parent = file.getParentFile();
			String pkg = null;
			if( parent != null )
				pkg = parent.getPath().replaceAll( "[\\\\/]", "." );

			String name = file.getName();
			Assert.isTrue( name.endsWith( ".jsp" ) );
			name = name.substring( 0, name.length() - 4 ) + "Servlet";
			File inputfile = new File( baseDir.getPath() + "/" + page );
			File outputfile = new File( new File( outputDir, parent.getPath() ), name + ".java" );

			if( inputfile.lastModified() > outputfile.lastModified() )
			{
				String script;
				FileReader in = new FileReader( baseDir.getPath() + "/" + page );
				try
				{
					script = new Parser().parse( new Scanner( in ), pkg, name );
				}
				finally
				{
					in.close();
				}
				outputfile.getParentFile().mkdirs();
				FileWriter out = new FileWriter( outputfile );
				try
				{
					out.write( script );
				}
				finally
				{
					out.close();
				}
				count++;
			}
		}
		if( count > 0 )
			System.out.println( "Precompiled " + count + " pages" );
	}

//	static public CompiledGroovyPage compile( Reader reader, String path, long lastModified )
//	{
//		log.info( "compile [" + path + "]" );
//		Matcher matcher = pathPattern.matcher( path );
//		Assert.isTrue( matcher.matches() );
//		path = matcher.group( 1 );
//		String name = matcher.group( 2 ).replaceAll( "[\\.-]", "_" );
//
//		String pkg = "ronnie.gosh.tmp.gpage";
//		if( path != null )
//			pkg += "." + path.replaceAll( "/", "." );
//
//		String script = new Parser().parse( new Scanner( reader ), pkg, name );
//		log.debug( "Generated groovy:\n" + script );
//
//		GroovyClassLoader loader = new GroovyClassLoader();
//		Class groovyClass = loader.parseClass( new GroovyCodeSource( script, name, "x" ) );
//		GroovyObject object = (GroovyObject)Util.newInstance( groovyClass );
//		return new CompiledGroovyPage( (Closure)object.invokeMethod( "getClosure", null ), lastModified );
//	}

	static protected class Scanner
	{
//		static final private Logger log = Logger.getLogger( Scanner.class );

		protected Reader reader;
		protected Stack< Integer > pushBack = new Stack();
		protected Stack< Integer > pushBackMarked;

		protected Scanner( Reader reader )
		{
			if( !reader.markSupported() )
				reader = new BufferedReader( reader );
			this.reader = reader;
		}

		protected int read()
		{
			if( this.pushBack.isEmpty() )
				try
			{
					int c = this.reader.read();
					if( c == 13 )
					{
						c = this.reader.read();
						if( c != 10 )
						{
							unread( c );
							c = 10;
						}
					}
					return c;
			}
			catch( IOException e )
			{
				throw new HttpException( e );
			}
			return this.pushBack.pop();
		}

		protected void unread( int c )
		{
			this.pushBack.push( c );
		}

		protected void mark( int readAheadLimit )
		{
			try
			{
				this.reader.mark( readAheadLimit );
			}
			catch( IOException e )
			{
				throw new HttpException( e );
			}
			this.pushBackMarked = (Stack)this.pushBack.clone();
		}

		protected void reset()
		{
			try
			{
				this.reader.reset();
			}
			catch( IOException e )
			{
				throw new HttpException( e );
			}
			this.pushBack = this.pushBackMarked;
			this.pushBackMarked = null;
		}

		protected String readWhitespace()
		{
			StringBuilder builder = new StringBuilder();
			int c = read();
			while( Character.isWhitespace( (char)c ) && c != '\n' )
			{
				builder.append( (char)c );
				c = read();
			}
			unread( c );
			return builder.toString();
		}
	}

	static protected class Parser
	{
//		static final private Logger log = Logger.getLogger( GroovyPageCompiler.class );

		protected Parser()
		{
			// Constructor
		}

		protected String parse( Scanner reader, String pkg, String cls )
		{
			Writer writer = new Writer( cls );
			writer.writeRaw( "package " + pkg + ";" );

//			log.trace( "-> parse" );
			String leading = reader.readWhitespace();
			int c = reader.read();
			while( c != -1 )
			{
				if( c == '<' )
				{
					c = reader.read();
					if( c == '%' )
					{
						reader.mark( 2 );
						c = reader.read();
						if( c == '=' )
						{
							writer.endDirectives();
							writer.writeWhiteSpaceAsString( leading ); leading = null;
							readScript( reader, writer, Mode.EXPRESSION );
						}
						else if( c == '-' && reader.read() == '-' )
						{
							writer.endDirectives();
							if( leading == null )
								readComment( reader );
							else
							{
								readComment( reader );
								String trailing = reader.readWhitespace();
								c = reader.read();
								if( (char)c == '\n' )
									// Comment on lines of his own, then ignore the lines.
									leading = reader.readWhitespace();
								else
								{
									reader.unread( c );
									writer.writeWhiteSpaceAsString( leading ); leading = null;
									writer.writeWhiteSpaceAsString( trailing );
								}
							}
						}
						else if( c == '@' )
						{
							readDirective( reader, writer );
						}
						else
						{
							writer.endDirectives();
							reader.reset();
							if( leading == null )
								readScript( reader, writer, Mode.SCRIPT );
							else
							{
								Writer writer2 = new Writer();
								writer2.mode = Mode.UNKNOWN;
								readScript( reader, writer2, Mode.SCRIPT );
								String trailing = reader.readWhitespace();
								c = reader.read();
								if( (char)c == '\n' )
								{
									// If script on lines of his own, add the whitespace and the newline to the script.
									writer.writeAsScript( leading ); leading = null;
									writer.writeAsScript( writer2.buffer );
									writer.writeAsScript( trailing );
									writer.writeAsScript( '\n' ); // Must not lose newlines
									leading = reader.readWhitespace();
								}
								else
								{
									reader.unread( c );
									writer.writeWhiteSpaceAsString( leading ); leading = null;
									writer.writeAsScript( writer2.buffer );
									writer.writeWhiteSpaceAsString( trailing );
								}
							}
						}
					}
					else
					{
						writer.endDirectives();
						writer.writeWhiteSpaceAsString( leading ); leading = null;
						writer.writeAsString( '<' );
						reader.unread( c );
					}
				}
				else
				{
					if( writer.mode != Mode.DIRECTIVES )
						writer.writeWhiteSpaceAsString( leading );
					leading = null;
					if( c == '$' )
					{
						writer.endDirectives();
						// TODO And without {}?
						c = reader.read();
						if( c == '{' )
							readGStringExpression( reader, writer );
						else if( c == '[' )
							readMessage( reader, writer, Mode.STRING );
						else
						{
							writer.writeAsString( '$' );
							reader.unread( c );
						}
					}
					else if( c == '\\' )
					{
						writer.endDirectives();
						c = reader.read();
						if( c == '$' )
						{
							writer.writeAsString( '\\' );
							writer.writeAsString( '$' );
						}
						else
						{
							writer.writeAsString( '\\' );
							writer.writeAsString( '\\' );
							reader.unread( c );
						}
					}
					else if( c == '"' )
					{
						writer.endDirectives();
						writer.writeAsString( '\\' );
						writer.writeAsString( (char)c );
					}
					else if( c == '\n' )
					{
						if( writer.mode == Mode.DIRECTIVES )
							writer.writeRaw( "\n" );
						else
							writer.writeAsString( (char)c );
						leading = reader.readWhitespace();
					}
					else
					{
						writer.endDirectives();
						writer.writeAsString( (char)c );
					}
				}

				c = reader.read();
			}
			writer.writeWhiteSpaceAsString( leading );

//			log.trace( "<- parse" );

			writer.endAll();

			writer.writeRaw( "}}" );

			return writer.getString();
		}

		protected void readScript( Scanner reader, Writer writer, Mode mode )
		{
			Assert.isTrue( mode == Mode.SCRIPT || mode == Mode.EXPRESSION );

//			log.trace( "-> readScript" );
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == '"' )
					readString( reader, writer, mode );
//				else if( c == '\'' )
//					readString( reader, writer, mode );
				else if( c == '%' )
				{
					c = reader.read();
					if( c == '>' )
						break;
					reader.unread( c );
					writer.writeAs( '%', mode );
				}
				else
					writer.writeAs( (char)c, mode );
			}
//			log.trace( "<- readScript" );
		}

		protected void readString( Scanner reader, Writer writer, Mode mode )
		{
//			log.trace( "-> readString" );
			writer.writeAs( '"', mode );
			boolean escaped = false;
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == '"' && !escaped )
					break;
				escaped = c == '\\';
				writer.writeAs( (char)c, mode );
			}
			writer.writeAs( '"', mode );
//			log.trace( "<- readString" );
		}

		protected String readString( Scanner reader )
		{
//			log.trace( "-> readString" );
			StringBuilder result = new StringBuilder();
			boolean escaped = false;
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == '"' && !escaped )
					return result.toString();
				escaped = c == '\\';
				if( !escaped )
					result.append( (char)c );
			}
		}

		protected void readGStringExpression( Scanner reader, Writer writer )
		{
			writer.endAll();
//			log.trace( "-> readEuh" );
//			writer.writeAs( '$', mode );
//			writer.writeAs( '{', mode );
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == '}' )
					break;
				if( c == '"' )
					readString( reader, writer, Mode.EXPRESSION2 );
//				else if( c == '\'' ) TODO This is important to, for example '}'
//					readString( reader, writer, Mode.EXPRESSION2 );
				else
					writer.writeAsExpression2( (char)c );
			}
//			writer.writeAs( '}', mode );
//			log.trace( "<- readEuh" );
		}

		protected void readMessage( Scanner reader, Writer writer, Mode mode )
		{
			writer.writeAs( "${message(\"", mode );
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == ']' )
					break;
				writer.writeAs( (char)c, mode );
			}
			writer.writeAs( "\")}", mode );
		}

		protected void readComment( Scanner reader )
		{
//			log.trace( "-> readComment" );
			while( true )
			{
				int c = reader.read();
				Assert.isTrue( c > 0 );
				if( c == '-' )
				{
					reader.mark( 10 );
					if( reader.read() == '-' && reader.read() == '%' && reader.read() == '>' )
						break;
					reader.reset();
				}
				else if( c == '<' )
				{
					reader.mark( 10 );
					if( reader.read() == '%' && reader.read() == '-' && reader.read() == '-' )
						readComment( reader );
					else
						reader.reset();
				}
			}
//			log.trace( "<- readComment" );
		}

		protected void readDirective( Scanner reader, Writer writer )
		{
			reader.readWhitespace();
			if( reader.read() != 'p' || reader.read() != 'a' || reader.read() != 'g' || reader.read() != 'e' )
				throw new HttpException( "Expecting 'page'" );
			reader.readWhitespace();
			if( reader.read() != 'i' || reader.read() != 'm' || reader.read() != 'p' || reader.read() != 'o' || reader.read() != 'r' || reader.read() != 't' )
				throw new HttpException( "Expecting 'import'" );
			reader.readWhitespace();
			if( reader.read() != '=' )
				throw new HttpException( "Expecting '='" );
			reader.readWhitespace();
			if( reader.read() != '"' )
				throw new HttpException( "Expecting '\"'" );
			String imp = readString( reader );
			reader.readWhitespace();
			if( reader.read() != '%' || reader.read() != '>' )
				throw new HttpException( "Expecting '%>'" );
			writer.writeImport( imp );
		}
	}

	static protected enum Mode { UNKNOWN, STRING, SCRIPT, EXPRESSION, EXPRESSION2, DIRECTIVES }

	static protected class Writer
	{
		protected StringBuilder buffer = new StringBuilder();
		protected Mode mode = Mode.UNKNOWN;
		protected String cls;

		protected Writer()
		{
			// Empty constructor
		}

		protected Writer( String cls )
		{
			this.cls = cls;
			this.mode = Mode.DIRECTIVES;
		}

		protected void writeAsString( char c )
		{
			endAllExcept( Mode.STRING );
			if( this.mode == Mode.UNKNOWN )
			{
				this.buffer.append( "writer.write(\"" );
				this.mode = Mode.STRING;
			}
			if( c == '\n' )
			{
				this.buffer.append( "\\n" );
				endAll();
			}
			this.buffer.append( c );
		}

		protected void writeWhiteSpaceAsString( CharSequence string )
		{
			if( string == null || string.length() == 0 )
				return;

			endAllExcept( Mode.STRING );
			if( this.mode == Mode.UNKNOWN )
			{
				this.buffer.append( "writer.write(\"" );
				this.mode = Mode.STRING;
			}
			this.buffer.append( string );
		}

		protected void writeAsExpression( char c )
		{
			endAllExcept( Mode.EXPRESSION );
			if( this.mode == Mode.UNKNOWN )
			{
				this.buffer.append( "writer.write(" );
				this.mode = Mode.EXPRESSION;
			}
			this.buffer.append( c );
		}

		protected void writeAsExpression2( char c )
		{
			endAllExcept( Mode.EXPRESSION2 );
			if( this.mode == Mode.UNKNOWN )
			{
				this.buffer.append( "writer.writeEncoded(" );
				this.mode = Mode.EXPRESSION2;
			}
			this.buffer.append( c );
		}

		protected void writeAsScript( char c )
		{
			endAllExcept( Mode.SCRIPT );
			if( this.mode == Mode.UNKNOWN )
				this.mode = Mode.SCRIPT;
			this.buffer.append( c );
		}

		// TODO What about newlines?
		protected void writeAsScript( CharSequence script )
		{
			if( script == null || script.length() == 0 )
				return;

			endAllExcept( Mode.SCRIPT );
			if( this.mode == Mode.UNKNOWN )
				this.mode = Mode.SCRIPT;
			this.buffer.append( script );
		}

		protected void writeAs( char c, Mode mode )
		{
			if( mode == Mode.EXPRESSION )
				writeAsExpression( c );
			else if( mode == Mode.EXPRESSION2 )
				writeAsExpression2( c );
			else if( mode == Mode.SCRIPT )
				writeAsScript( c );
			else if( mode == Mode.STRING )
				writeAsString( c );
			else
				Assert.fail( "Mode not allowed: " + mode );
		}

		// TODO What about newlines?
		protected void writeAs( CharSequence string, Mode mode )
		{
			if( string == null || string.length() == 0 )
				return;

			if( mode == Mode.EXPRESSION || mode == Mode.EXPRESSION2 )
				Assert.fail( "mode EXPRESSION not allowed" );
			else if( mode == Mode.SCRIPT )
				writeAsScript( string );
			else if( mode == Mode.STRING )
				writeWhiteSpaceAsString( string );
			else
				Assert.fail( "mode UNKNOWN not allowed" );
		}

		protected void writeImport( String imp )
		{
			Assert.isTrue( this.mode == Mode.DIRECTIVES );
			writeRaw( "import " + imp + ";" );
		}

		protected void writeRaw( String s )
		{
			this.buffer.append( s );
		}

		private void endExpression()
		{
			Assert.isTrue( this.mode == Mode.EXPRESSION );
			this.buffer.append( ");" );
			this.mode = Mode.UNKNOWN;
		}

		private void endExpression2()
		{
			Assert.isTrue( this.mode == Mode.EXPRESSION2 );
			this.buffer.append( ");" );
			this.mode = Mode.UNKNOWN;
		}

		private void endScript()
		{
			Assert.isTrue( this.mode == Mode.SCRIPT );
//			this.buffer.append( ';' );
			this.mode = Mode.UNKNOWN;
		}

		private void endString()
		{
			Assert.isTrue( this.mode == Mode.STRING );
			this.buffer.append( "\");" );
			this.mode = Mode.UNKNOWN;
		}

		protected void endDirectives()
		{
			if( this.mode == Mode.DIRECTIVES )
			{
				this.mode = Mode.UNKNOWN;
				writeRaw( "public class " + this.cls + " implements Servlet{public void call(RequestContext request,Parameters params){" );
			}
		}

		private void endAllExcept( Mode mode )
		{
			if( this.mode == mode )
				return;

			if( this.mode == Mode.STRING )
				endString();
			else if( this.mode == Mode.EXPRESSION )
				endExpression();
			else if( this.mode == Mode.EXPRESSION2 )
				endExpression2();
			else if( this.mode == Mode.SCRIPT )
				endScript();
			else if( this.mode == Mode.DIRECTIVES )
				Assert.fail( "Can't end DIRECTIVES" );
		}

		protected void endAll()
		{
			endAllExcept( null );
		}

		protected String getString()
		{
			return this.buffer.toString();
		}
	}
}
