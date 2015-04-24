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

package solidbase.test;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener
{
	public void onFinish( ITestContext arg0 )
	{
		// Nothing
	}

	public void onStart( ITestContext arg0 )
	{
		// Nothing
	}

	public void onTestFailedButWithinSuccessPercentage( ITestResult arg0 )
	{
		// Nothing
	}

	public void onTestFailure( ITestResult arg0 )
	{
		// Nothing
	}

	public void onTestSkipped( ITestResult arg0 )
	{
		// Nothing
	}

	public void onTestStart( ITestResult result )
	{
		System.out.println( "---------- " + result.getName() + " ----------" );
	}

	public void onTestSuccess( ITestResult arg0 )
	{
		// Nothing
	}
}
