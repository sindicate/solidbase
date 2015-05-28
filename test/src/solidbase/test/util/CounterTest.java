package solidbase.test.util;

import solidbase.util.LogCounter;
import solidbase.util.ProgressiveIntervalLogCounter;

public class CounterTest
{
	static public void main( String[] args )
	{
		LogCounter counter = new ProgressiveIntervalLogCounter( 10, 1000, 100000 );
//		Counter counter = new FixedCounter( 100000 );
//		Counter counter = new SilentCounter();
		for( int i = 0; i < 1000000; i++ )
		{
			if( counter.next() )
				System.out.println( "Done " + counter.total() );
		}
		System.out.println( "Done " + counter.total() );
	}
}
