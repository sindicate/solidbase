/*--
 * Copyright 2010 René M. de Bloois
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

package solidbase.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;

import solidbase.core.FatalException;
import solidbase.core.Runner;
import solidstack.io.Resource;
import solidstack.io.Resources;
import solidstack.script.ScriptException;


/**
 * The Sql Ant Task.
 *
 * @author René M. de Bloois
 */
public class SQLTask extends DBTask
{
	/**
	 * Field to store the sqlfile attribute.
	 */
	protected String sqlfile;

	/**
	 * Field to store multiple nested sqlfile elements.
	 */
	protected List< Sqlfile > sqlfiles = new ArrayList<>();

	/**
	 * Sets the sqlfile attribute.
	 *
	 * @param sqlfile The sqlfile attribute.
	 */
	public void setSqlfile( String sqlfile )
	{
		this.sqlfile = sqlfile;
	}

	/**
	 * Creates a nested sqlfile element.
	 *
	 * @return The nested sqlfile element.
	 */
	public Sqlfile createSqlfile()
	{
		Sqlfile sqlfile = new Sqlfile();
		this.sqlfiles.add( sqlfile );
		return sqlfile;
	}

	/**
	 * Validates the configuration of the Ant Task.
	 */
	@Override
	protected void validate()
	{
		super.validate();

		if( this.sqlfile == null )
		{
			if( this.sqlfiles.isEmpty() )
				throw new BuildException( "The " + getTaskName() + " task needs the 'sqlfile' attribute or nested 'sqlfile' elements" );
		}
		else if( !this.sqlfiles.isEmpty() )
			throw new BuildException( "The " + getTaskName() + " task does not accept both the 'sqlfile' attribute and nested 'sqlfile' elements" );
	}

	@Override
	public Runner prepareRunner()
	{
		Runner runner = super.prepareRunner();

		List< Resource > sqlFiles = new ArrayList<>();
		File baseDir = getProject().getBaseDir();
		if( this.sqlfile != null )
			sqlFiles.add( Resources.getResource( baseDir ).resolve( this.sqlfile ) );
		for( Sqlfile file : this.sqlfiles )
			sqlFiles.add( Resources.getResource( baseDir ).resolve( file.src ) );
		runner.setSQLFiles( sqlFiles );

		return runner;
	}

	@Override
	public void execute()
	{
		validate();

		Runner runner = prepareRunner();
		try
		{
			runner.executeSQL();
		}
		catch( FatalException e )
		{
			// TODO When debugging, we should give the whole exception, not only the message
			// TODO Shouldn't we just wrap the exception, and then Ant is the one who decides if it only shows the message or the complete stacktrace?
			throw new BuildException( e.getMessage() );
		}
		catch( ScriptException e )
		{
			// TODO Or should ScriptException be wrapped in a FatalException?
			throw new BuildException( e.getMessage() );
		}
	}

	/**
	 * Object used to configure the nested sqlfile element of the SQLTask.
	 *
	 * @author René de Bloois
	 */
	static protected class Sqlfile
	{
		/**
		 * The file path.
		 */
		protected String src;

		/**
		 * Constructor.
		 */
		public Sqlfile()
		{
			super();
		}

		/**
		 * Constructor.
		 *
		 * @param src The file path.
		 */
		public Sqlfile( String src )
		{
			this.src = src;
		}

		/**
		 * Sets the file path.
		 *
		 * @param src The file path.
		 */
		public void setSrc( String src )
		{
			this.src = src;
		}
	}
}
