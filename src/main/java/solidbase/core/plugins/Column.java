/*--
 * Copyright 2015 Ren� M. de Bloois
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

import solidbase.util.JDBCSupport;


public class Column
{
	private String name;
	private int type;
	private String table;
	private String schema;
	private String typeName;


	public Column( String name, int type, String table, String schema )
	{
		this.name = name;
		this.type = type;
		this.table = table;
		this.schema = schema;
		this.typeName = JDBCSupport.toTypeName( type );
	}

	public int getType()
	{
		return this.type;
	}

	public void setType( int type )
	{
		this.type = type;
	}

	public String getName()
	{
		return this.name;
	}

	public String getSchema()
	{
		return this.schema;
	}

	public String getTable()
	{
		return this.table;
	}

	public String getTypeName()
	{
		return this.typeName;
	}
}
