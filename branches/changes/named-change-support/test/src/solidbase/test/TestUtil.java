/*--
 * Copyright 2009 Ren� M. de Bloois
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

package solidbase.test;

public class TestUtil
{
	static public String generalizeOutput( String output )
	{
		output = output.replaceAll( "file:/\\S+/", "file:/.../" );
		output = output.replaceAll( "C:\\\\\\S+\\\\", "C:\\\\...\\\\" );
		output = output.replaceAll( "SolidBase v1\\.5\\.x\\s+\\(C\\) 2006-200\\d Ren� M\\. de Bloois", "SolidBase v1.5.x (C) 2006-200x Ren� M. de Bloois" );
		output = output.replaceAll( "jdbc:derby:c:/\\S+;", "jdbc:derby:c:/...;" );
		return output.replaceAll( "\\\r", "" );
	}
}
