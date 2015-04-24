package ronnie.dbpatcher.test;

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
