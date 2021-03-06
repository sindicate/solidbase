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

package solidbase.maven;

/**
 * Parameter object to allow configuring of parameters.
 *
 * @author René de Bloois
 */
public class Parameter
{
	private String name;
	private String value;

	/**
	 *
	 */
	public Parameter()
	{
	}

	/**
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 */
	public Parameter( String name, String value )
	{
		this.name = name;
		this.value = value;
	}

	/**
	 * @return The name of the parameter.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Sets the name of the parameter.
	 * @param name The name for the parameter.
	 */
	public void setName( String name )
	{
		this.name = name;
	}

	/**
	 * @return The value of the parameter.
	 */
	public String getValue()
	{
		return this.value;
	}

	/**
	 * Set the value of the parameter.
	 * @param value The value of the parameter.
	 */
	// TODO Text as value, but Ant does not substitute placeholders in the text element.
	public void setValue( String value )
	{
		this.value = value;
	}
}
