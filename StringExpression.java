/*--
 * Copyright 2012 René M. de Bloois
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

package solidbase.core.script;

import java.util.ArrayList;
import java.util.List;



public class StringExpression
{
	private List<String> script = new ArrayList<String>();
	private List<Fragment> fragments = new ArrayList<Fragment>();


//	public StringExpression( SourceLocation location )
//	{
//		super( location );
//	}
//
//	public Expression compile()
//	{
//		if( this.fragments.size() == 0 )
//			return new StringLiteral( getLocation(), "" );
//		if( this.fragments.size() == 1 && this.fragments.get( 0 ) != null )
//			return new StringLiteral( getLocation(), this.fragments.get( 0 ) );
//
//		ListIterator<Expression> i = this.expressions.listIterator();
//		while( i.hasNext() )
//			i.set( i.next().compile() );
//
//		return this;
//	}
//
//	public PString evaluate( ThreadContext thread )
//	{
//		List<String> fragments = new ArrayList<String>(); // TODO Or LinkedList?
//		List<Object> values = new ArrayList<Object>();
//		int i = 0;
//		for( String fragment : this.fragments )
//		{
//			if( fragment != null )
//				fragments.add( fragment );
//			else
//			{
//				Object object = this.expressions.get( i++ ).evaluate( thread );
//				values.add( object );
//				fragments.add( null ); // This is the value indicator
//			}
//		}
//		return new PString( fragments.toArray( new String[ fragments.size() ] ), values.toArray() );
//	}

	public void appendFragment( Fragment fragment )
	{
		this.fragments.add( fragment );
	}

	public void append( String expression )
	{
		this.script.add( expression );
		this.fragments.add( null );
	}

//	public void writeTo( StringBuilder out )
//	{
//		for( Expression expression : this.expressions )
//		{
//			expression.writeTo( out );
//			out.append( '+' );
//		}
//	}
}
