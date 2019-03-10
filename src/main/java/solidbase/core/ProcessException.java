/*--
 * Copyright 2009 René M. de Bloois
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

package solidbase.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import solidstack.io.SourceLocation;


/**
 * An {@link SQLException} has occurred during execution of a {@link Command}. As a subclass of {@link FatalException}
 * the message of this exception will be presented to the user, not the stack trace.
 *
 * @author René M. de Bloois
 */
public class ProcessException extends FatalException
{
	private static final long serialVersionUID = 1L;

	private List<Object> hierarchy = new ArrayList<>();


	/**
	 * Constructor.
	 *
	 * @param cause The cause.
	 */
	public ProcessException( Throwable cause )
	{
		super( cause );

		if( cause == null )
			throw new NullPointerException( "cause == null" );
	}

	public ProcessException addProcess( String process )
	{
		if( process == null )
			throw new NullPointerException( "process == null" );
		this.hierarchy.add( process );
		return this;
	}

	public ProcessException addLocation( SourceLocation location )
	{
		if( location == null )
			throw new NullPointerException( "location == null" );
		this.hierarchy.add( location );
		return this;
	}

	/**
	 * Loops through all the exceptions contained in the {@link SQLException} and combines all messages and SQLStates into one String.
	 *
	 * @return all messages and SQLStates from the {@link SQLException} combined into one string.
	 * @see SQLException#getNextException()
	 */
	// TODO When BatchUpdateException, in Derby we get the message twice because of BatchUpdatException composing it from the exception chain. And in Oracle?
	private  String getSQLErrorMessages( SQLException e )
	{
		StringBuilder result = new StringBuilder();
		while( true )
		{
			String sqlState = e.getSQLState();
			if( sqlState != null )
				result.append( sqlState ).append( ": " );
			result.append( e.getMessage() );
			e = e.getNextException();
			if( e == null )
				break;
			result.append( "\n" );
		}
		return result.toString();
	}

	@Override
	public String getMessage()
	{
		Throwable t = getCause();
		String message = t instanceof SQLException ? getSQLErrorMessages( (SQLException)t ) : t.getMessage();

		StringBuilder result = new StringBuilder( message );
		for( Object object : this.hierarchy )
			if( object instanceof SourceLocation )
				result.append( "\nAt " ).append( object );
			else
				result.append( "\nWhile " ).append( object.toString() );
		return result.toString();
	}
}
