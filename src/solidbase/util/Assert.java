/*--
 * Copyright 2005 Ren� M. de Bloois
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

package solidbase.util;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

/**
 * Assert methods.
 *
 * @author Ren� M. de Bloois
 * @since Jan 5, 2005
 */
public class Assert
{
	/**
	 * This utility class cannot be constructed.
	 */
	private Assert()
	{
		super();
	}

	/**
	 * Asserts that the <code>test</code> argument is <code>true</code>. If not, it throws an
	 * {@link AssertionFailedException}.
	 *
	 * @param test
	 *            Test argument.
	 */
	static public void isTrue( boolean test )
	{
		if( !test ) {
			throwAssertionFailure( null, 2 );
		}
	}

	/**
	 * Assert that the <code>test</code> argument is <code>true</code>. If not, it throws an
	 * {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void isTrue( boolean test, String errorMessage )
	{
		if( !test ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Assert that the <code>test</code> argument is <code>true</code>. If not, it throws an
	 * {@link AssertionFailedException} with the given <code>errorMessage</code> and the given linenumber.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 * @param linenumber
	 *            The line number to show in the error message.
	 */
	static public void isTrue( boolean test, String errorMessage, int linenumber )
	{
		if( !test ) {
			throwAssertionFailure( errorMessage + ", line " + linenumber, 2 );
		}
	}

	/**
	 * Assert that the <code>test</code> argument is <code>false</code>. If not, it throws an
	 * {@link AssertionFailedException}.
	 *
	 * @param test
	 *            Test argument.
	 */
	static public void isFalse( boolean test )
	{
		if( test ) {
			throwAssertionFailure( null, 2 );
		}
	}

	/**
	 * Assert that the <code>test</code> argument is <code>false</code>. If not, it throws an
	 * {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void isFalse( boolean test, String errorMessage )
	{
		if( test ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Throws an {@link AssertionFailedException} with the given <code>errorMessage</code> while popping the number of
	 * items from the stack trace. This gives the impression that the exception is generated from the caller's code.
	 *
	 * @param errorMessage
	 *            The error message.
	 * @param pop
	 *            How many items must be popped off the stack trace.
	 */
	static protected void throwAssertionFailure( String errorMessage, int pop )
	{
		AssertionFailedException e = new AssertionFailedException( errorMessage );
		StackTraceElement[] oldElements = e.getStackTrace();
		StackTraceElement[] newElements = new StackTraceElement[ oldElements.length - pop ];
		System.arraycopy( oldElements, pop, newElements, 0, newElements.length );
		e.setStackTrace( newElements );
		throw e;
	}

	/**
	 * Throws an {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param errorMessage
	 *            The error message.
	 */
	static public void fail( String errorMessage )
	{
		throwAssertionFailure( errorMessage, 2 );
	}

	/**
	 * Throws an {@link AssertionFailedException}.
	 */
	static public void fail()
	{
		throwAssertionFailure( null, 2 );
	}

	/**
	 * Assert that String argument s is not null and not "". If not, it throws an {@link AssertionFailedException}.
	 *
	 * @param test
	 *            Test argument.
	 */
	static public void notEmpty( String test )
	{
		notEmpty( test, null );
	}

	/**
	 * Assert that String argument s is not null and not "". If not, it throws an {@link AssertionFailedException} with
	 * the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void notEmpty( String test, String errorMessage )
	{
		if( StringUtils.isEmpty( test ) ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Assert that the collection is not null and not empty. If not, an {@link AssertionFailedException} is thrown.
	 *
	 * @param collection The collection to be checked for not emptiness.
	 */
	static public void notEmpty( Collection< ? > collection )
	{
		if( collection == null || collection.isEmpty() ) {
			throwAssertionFailure( null, 2 );
		}
	}

	/**
	 * Assert that String argument s is not null and not "" and not whitespace. If not, it throws an
	 * {@link AssertionFailedException}.
	 *
	 * @param test
	 *            Test argument.
	 */
	static public void notBlank( String test )
	{
		notBlank( test, null );
	}

	/**
	 * Assert that String argument s is not null and not "" and not whitespace. If not, it throws an
	 * {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void notBlank( String test, String errorMessage )
	{
		if( StringUtils.isBlank( test ) ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Asserts that the <code>test</code> argument is not <code>null</code>. If not, it throws an {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void notNull( Object test, String errorMessage )
	{
		if( test == null ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Asserts that the <code>test</code> argument is <code>null</code>. If not, it throws an {@link AssertionFailedException}.
	 *
	 * @param test
	 *            Test argument.
	 */
	static public void isNull( Object test )
	{
		if( test != null ) {
			throwAssertionFailure( null, 2 );
		}
	}

	/**
	 * Asserts that the <code>test</code> argument is <code>null</code>. If not, it throws an {@link AssertionFailedException} with the given <code>errorMessage</code>.
	 *
	 * @param test
	 *            Test argument.
	 * @param errorMessage
	 *            The error message.
	 */
	static public void isNull( Object test, String errorMessage )
	{
		if( test != null ) {
			throwAssertionFailure( errorMessage, 2 );
		}
	}

	/**
	 * Asserts that the {@code object} is an instance of the {@code class}. If not, an
	 * {@link AssertionFailedException} is thrown.
	 *
	 * @param object The object of which the type is checked.
	 * @param type The type to be checked.
	 */
	static public void isInstanceOf( Object object, Class< ? > type )
	{
		if( !type.isInstance( object ) ) {
			throwAssertionFailure( null, 2 );
		}
	}

	/**
	 * Asserts that the {@code object} is not an instance of the {@code class}. If it is, an
	 * {@link AssertionFailedException} is thrown.
	 *
	 * @param object The object of which the type is checked.
	 * @param type The type to be checked.
	 */
	static public void notInstanceOf( Object object, Class< ? > type )
	{
		if( type.isInstance( object ) ) {
			throwAssertionFailure( null, 2 );
		}
	}
}
