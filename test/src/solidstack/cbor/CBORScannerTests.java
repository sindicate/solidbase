package solidstack.cbor;

import java.io.ByteArrayOutputStream;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;


public class CBORScannerTests
{
	@Test
	public void testIArray()
	{
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( byteArray );

		out.startArray();
		out.writeText( "text1" );
		out.writeText( "text1" );
		out.writeBreak();

		CBORToString toString = new CBORToString( byteArray.toByteArray() );
		String s = toString.toString();
		System.out.println( s );
		Assertions.assertThat( s ).isEqualTo( "IARRAY\n"
				+ "    TEXT 05: \"text1\"\n"
				+ "    TEXT 05: \"text1\"\n"
				+ "BREAK\n" );
	}

	@Test
	public void testStringRef()
	{
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( byteArray );

		out.tagRefNS();
		out.startArray();
		out.writeText( "text1" );
		out.writeText( "te1" );
		out.writeText( "text1" );
		out.writeText( "te1" );
		out.writeText( "t2" );
		out.writeText( "t2" );
		out.startArray();
		out.writeText( "text1" );
		out.writeText( "te1" );
		out.writeText( "t2" );
		out.writeBreak();
		out.writeBreak();

		CBORToString toString = new CBORToString( byteArray.toByteArray() );
		String s = toString.toString();
		System.out.println( s );
		Assertions.assertThat( s ).isEqualTo( "TAG 0100 IARRAY\n"
				+ "    TEXT 05: \"text1\"\n"
				+ "    TEXT 03: \"te1\"\n"
				+ "    TAG 19 UINT 00\n"
				+ "    TAG 19 UINT 01\n"
				+ "    TEXT 02: \"t2\"\n"
				+ "    TEXT 02: \"t2\"\n"
				+ "    IARRAY\n"
				+ "        TAG 19 UINT 00\n"
				+ "        TAG 19 UINT 01\n"
				+ "        TEXT 02: \"t2\"\n"
				+ "    BREAK\n"
				+ "BREAK\n" );
	}

	@Test
	public void testNestedArray()
	{
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( byteArray );

		out.startArray();
		out.writeText( "test1" );
		out.writeText( "test1" );
		out.startArray();
		out.writeText( "test2" );
		out.writeText( "test2" );
		out.writeBreak();
		out.writeBreak();

		CBORToString toString = new CBORToString( byteArray.toByteArray() );
		String s = toString.toString();
		System.out.println( s );
		Assertions.assertThat( s ).isEqualTo( "IARRAY\n"
				+ "    TEXT 05: \"test1\"\n"
				+ "    TEXT 05: \"test1\"\n"
				+ "    IARRAY\n"
				+ "        TEXT 05: \"test2\"\n"
				+ "        TEXT 05: \"test2\"\n"
				+ "    BREAK\n"
				+ "BREAK\n" );
	}

	@Test
	public void testNestedMap()
	{
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( byteArray );

		out.startMap();
		out.writeText( "test1" );
		out.writeText( "test1" );
		out.startMap();
		out.writeText( "test2" );
		out.writeText( "test2" );
		out.writeBreak();
		out.writeBreak();

		CBORToString toString = new CBORToString( byteArray.toByteArray() );
		String s = toString.toString();
		System.out.println( s );
		Assertions.assertThat( s ).isEqualTo( "IMAP\n"
				+ "    TEXT 05: \"test1\"\n"
				+ "    TEXT 05: \"test1\"\n"
				+ "    IMAP\n"
				+ "        TEXT 05: \"test2\"\n"
				+ "        TEXT 05: \"test2\"\n"
				+ "    BREAK\n"
				+ "BREAK\n" );
	}
}
