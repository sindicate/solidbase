package com.logicacmg.idt.commons.io;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import org.testng.annotations.Test;

public class RandomAccessLineReaderTests
{
	@Test
	public void testUtf16BomAndExplicit() throws IOException, SQLException
	{
		URL url = RandomAccessLineReaderTests.class.getResource( "/patch-utf-16-bom-2.sql" );
		RandomAccessLineReader reader = new RandomAccessLineReader( url );
		System.out.println( "First line [" + reader.readLine() + "]" );
		assert reader.bomSize == 2;
		assert reader.encoding.equals( "UTF-16LE" );
	}
}
