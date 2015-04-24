/*--
 * Copyright 2006 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.core.plugins;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.mindprod.csv.CSVReader;

import solidbase.core.Command;
import solidbase.core.CommandFileException;
import solidbase.core.plugins.ImportCSV.Parsed;

public class Import
{
	@Test( groups="new" )
	public void testStatementParsing1()
	{
		String sql = "IMPORT CSV INTO TEMP DATA\n";
		Parsed parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		assert parsed.columns == null;
		assert parsed.lineNumber == 2;
		assert parsed.prependLineNumber == false;
		assert parsed.separator == ',';
		assert parsed.tableName.equals( "TEMP" );
		assert parsed.usePLBlock == false;
		assert parsed.useValuesList == false;
		assert parsed.values == null;

		sql = "IMPORT CSV SEPARATED BY TAB INTO TEMP\nDATA\n";
		parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		assert parsed.columns == null;
		assert parsed.lineNumber == 3;
		assert parsed.prependLineNumber == false;
		assert parsed.separator == '\t';
		assert parsed.tableName.equals( "TEMP" );
		assert parsed.usePLBlock == false;
		assert parsed.useValuesList == false;
		assert parsed.values == null;

		sql = "IMPORT CSV SEPARATED BY ; PREPEND LINENUMBER INTO TEMP2 DATA\n";
		parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		assert parsed.columns == null;
		assert parsed.lineNumber == 2;
		assert parsed.prependLineNumber == true;
		assert parsed.separator == ';';
		assert parsed.tableName.equals( "TEMP2" );
		assert parsed.usePLBlock == false;
		assert parsed.useValuesList == false;
		assert parsed.values == null;

		sql = "IMPORT CSV\nSEPARATED BY |\nINTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )\nVALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-\", TEST ''X' )\nDATA\n";
		parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		assert parsed.columns != null;
		Assert.assertEquals( parsed.lineNumber, 6 );
		assert parsed.prependLineNumber == false;
		assert parsed.separator == '|';
		assert parsed.tableName.equals( "TEMP3" );
		assert parsed.usePLBlock == false;
		assert parsed.useValuesList == false;
		assert parsed.values != null;
		Assert.assertEquals( parsed.columns, new String[] { "TEMP1", "TEMP2", "TEMP3", "TEMP4" } );
		Assert.assertEquals( parsed.values, new String[] { ":2", "CONVERT( :1, INTEGER ) + :2", "'Y'", "'-)-\", TEST ''X'" } );

		sql = "IMPORT CSV INTO TEMP3 ( TEMP1, TEMP2, TEMP3 ) VALUES ( 1, 2, 3, 4 )";
		try
		{
			ImportCSV.parse( new Command( sql, false, 1 ) );
			Assert.fail( "Expecting a CommandFileException" );
		}
		catch( CommandFileException e )
		{
			Assert.assertTrue( e.getMessage().contains( "Number of specified columns does not match number of given values, at line " ) );
		}
	}

	public String generateSQLUsingPLBlock( String sql ) throws IOException
	{
		Parsed parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		CSVReader reader = new CSVReader( parsed.reader, parsed.separator, '"', "#", true, false, true );
		String[] line = reader.getAllFieldsInLine();
		String result = ImportCSV.generateSQLUsingPLBlock( reader, parsed, line );
		System.out.println( result );
		return result;
	}

	@Test( groups="new" )
	public void testSQLGeneration1() throws IOException
	{
		String sql = generateSQLUsingPLBlock( "IMPORT CSV INTO TEMP DATA\n" +
				"	\"1\", \"2\", \"3\"\n"
		);
		Assert.assertEquals( sql, "BEGIN\n" +
				"INSERT INTO TEMP VALUES ('1','2','3');\n" +
				"END;\n"
		);
		sql = generateSQLUsingPLBlock( "IMPORT CSV SEPARATED BY TAB INTO TEMP\n" +
				"DATA\n" +
				"\"1\"	\"2\"	\"3\"" +
				"\n\"4\"	\"5\"	\"6\"\n"
		);
		Assert.assertEquals( sql, "BEGIN\n" +
				"INSERT INTO TEMP VALUES ('1','2','3');\n" +
				"INSERT INTO TEMP VALUES ('4','5','6');\n" +
				"END;\n"
		);
		sql = generateSQLUsingPLBlock( "IMPORT CSV SEPARATED BY ; PREPEND LINENUMBER INTO TEMP2 DATA\n" +
				"\"1\"; \"2\"; \"3\"" +
				"\n\"4\"; \"5\"; \"6\"\n"
		);
		Assert.assertEquals( sql, "BEGIN\n" +
				"INSERT INTO TEMP2 VALUES (2,'1','2','3');\n" +
				"INSERT INTO TEMP2 VALUES (3,'4','5','6');\n" +
				"END;\n"
		);
		sql = generateSQLUsingPLBlock( "IMPORT CSV\n" +
				"SEPARATED BY |\n" +
				"INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )\n" +
				"VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-\", TEST ''X' )\n" +
				"DATA\n" +
				"1|2\n" +
				"3|4\n"
		);
		Assert.assertEquals( sql, "BEGIN\n" +
				"INSERT INTO TEMP3 (TEMP1,TEMP2,TEMP3,TEMP4) VALUES ('2',CONVERT( '1', INTEGER ) + '2','Y','-)-\", TEST ''X');\n" +
				"INSERT INTO TEMP3 (TEMP1,TEMP2,TEMP3,TEMP4) VALUES ('4',CONVERT( '3', INTEGER ) + '4','Y','-)-\", TEST ''X');\n" +
				"END;\n"
		);
		sql = generateSQLUsingPLBlock( "IMPORT CSV INTO TEMP3 VALUES ( SEQUENCE.NEXTVAL, 'TEST' ) DATA\n" +
				"1|2\n" +
				"3|4\n"
		);
		Assert.assertEquals( sql, "BEGIN\n" +
				"INSERT INTO TEMP3 VALUES (SEQUENCE.NEXTVAL,'TEST');\n" +
				"INSERT INTO TEMP3 VALUES (SEQUENCE.NEXTVAL,'TEST');\n" +
				"END;\n"
		);
	}

	public String generateSQLUsingValuesList( String sql ) throws IOException
	{
		Parsed parsed = ImportCSV.parse( new Command( sql, false, 1 ) );
		CSVReader reader = new CSVReader( parsed.reader, parsed.separator, '"', "#", true, false, true );
		String[] line = reader.getAllFieldsInLine();
		String result = ImportCSV.generateSQLUsingValuesList( reader, parsed, line );
		System.out.println( result );
		return result;
	}

	@Test( groups="new" )
	public void testSQLGeneration2() throws IOException
	{
		String sql = generateSQLUsingValuesList( "IMPORT CSV INTO TEMP DATA\n" +
				"	\"1\", \"2\", \"3\"\n"
		);
		Assert.assertEquals( sql, "INSERT INTO TEMP VALUES ('1','2','3')\n"
		);
		sql = generateSQLUsingValuesList( "IMPORT CSV SEPARATED BY TAB INTO TEMP\n" +
				"DATA\n" +
				"\"1\"	\"2\"	\"3\"" +
				"\n\"4\"	\"5\"	\"6\"\n"
		);
		Assert.assertEquals( sql, "INSERT INTO TEMP VALUES ('1','2','3'),\n" +
				"('4','5','6')\n"
		);
		sql = generateSQLUsingValuesList( "IMPORT CSV SEPARATED BY ; PREPEND LINENUMBER INTO TEMP2 DATA\n" +
				"\"1\"; \"2\"; \"3\"" +
				"\n\"4\"; \"5\"; \"6\"\n"
		);
		Assert.assertEquals( sql, "INSERT INTO TEMP2 VALUES (2,'1','2','3'),\n" +
				"(3,'4','5','6')\n"
		);
		sql = generateSQLUsingValuesList( "IMPORT CSV\n" +
				"SEPARATED BY |\n" +
				"INTO TEMP3 ( TEMP1, TEMP2, TEMP3, TEMP4 )\n" +
				"VALUES ( :2, CONVERT( :1, INTEGER ) + :2, 'Y', '-)-\", TEST ''X' )\n" +
				"DATA\n" +
				"1|2\n" +
				"3|4\n"
		);
		Assert.assertEquals( sql, "INSERT INTO TEMP3 (TEMP1,TEMP2,TEMP3,TEMP4) VALUES ('2',CONVERT( '1', INTEGER ) + '2','Y','-)-\", TEST ''X'),\n" +
				"('4',CONVERT( '3', INTEGER ) + '4','Y','-)-\", TEST ''X')\n"
		);
	}
}
