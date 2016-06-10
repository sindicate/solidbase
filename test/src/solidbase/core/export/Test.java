package solidbase.core.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import solidstack.cbor.CBORToString;
import solidstack.io.Resources;

public class Test
{
	static public void main( String... args ) throws IOException
	{
		InputStream in = Resources.getResource( "D:/solidbase/complaints-400000.cbor" ).newInputStream();
		in = new BufferedInputStream( in, 0x1000 );
		CBORToString toString = new CBORToString( in );

		OutputStream out = new FileOutputStream( "D:/solidbase/complaints-400000.cbor.txt" );
		out = new BufferedOutputStream( out, 0x1000 );
		Writer w = new OutputStreamWriter( out, Charset.forName( "UTF-8" ) );

		toString.toString( w );

		w.close();
		in.close();
	}
}
