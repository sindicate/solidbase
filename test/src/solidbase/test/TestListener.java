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

import java.util.List;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

public class TestListener implements IInvokedMethodListener, IReporter
{
	@Override
	public void afterInvocation( IInvokedMethod arg0, ITestResult arg1 )
	{
		// Nothing
	}

	@Override
	public void beforeInvocation( IInvokedMethod method, ITestResult result )
	{
		System.out.println( "---------- " + method.getTestMethod().getInstance().getClass().getName() + "." + method.getTestMethod().getMethodName() + "() ----------" );
	}

	@Override
	public void generateReport( List<XmlSuite> arg0, List<ISuite> arg1, String arg2 ) {
	}

}
