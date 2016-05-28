package solidstack.cbor;

import java.io.ByteArrayOutputStream;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;


public class CBOROutputStreamTests
{
	@Test
	public void test1()
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( bytes );

		out.startArray();
		out.writeText( "text1" );
		out.writeText( "text1" );
		out.writeBreak();

//		printHex( bytes.toByteArray() );
		Assertions.assertThat( bytes.toByteArray() ).containsExactly( toBytes( "9F 65 74 65 78 74 31 65 74 65 78 74 31 FF" ) );
	}

	@Test
	public void test2()
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		CBORWriter out = new CBORWriter( bytes );

		out.tagRefNS();
		out.startArray();
		out.writeText( "te1" );
		out.writeText( "te1" );
		out.writeText( "t2" );
		out.writeText( "t2" );
		out.writeBreak();

//		printHex( bytes.toByteArray() );
		Assertions.assertThat( bytes.toByteArray() ).containsExactly( toBytes( "D9 01 00 9F 63 74 65 31 D8 19 00 62 74 32 62 74 32 FF" ) );
	}

	private String toHex( int b )
	{
		String result = Integer.toHexString( b & 0xFF ).toUpperCase();
		if( result.length() < 2 )
			result = '0' + result;
		return result;
	}

	private void printHex( byte[] bytes )
	{
//		for( byte b : bytes )
//			System.out.print( "(byte)0x" + toHex( b ) + ", " );
//		System.out.println();
		for( byte b : bytes )
			System.out.print( toHex( b ) + ' ' );
		System.out.println();
	}

	private byte[] toBytes( String hex )
	{
		String[] hs = hex.split( " " );
		byte[] result = new byte[ hs.length ];
		int i = 0;
		for( String h : hs )
			result[ i++ ] = (byte)Integer.parseInt( h, 16 );
		return result;
	}
}
