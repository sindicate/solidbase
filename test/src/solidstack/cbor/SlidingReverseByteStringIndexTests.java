package solidstack.cbor;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;


public class SlidingReverseByteStringIndexTests
{
	@Test
	public void test1()
	{
		SlidingReverseByteStringIndex index = new SlidingReverseByteStringIndex( 4, 64 );
		CBORByteString b1 = new CBORByteString( false, new byte[] { 1, 0, 0 } );
		CBORByteString b2 = new CBORByteString( false, new byte[] { 2, 0, 0 } );
		CBORByteString b3 = new CBORByteString( false, new byte[] { 3, 0, 0 } );
		CBORByteString b4 = new CBORByteString( false, new byte[] { 4, 0, 0 } );
		CBORByteString b5 = new CBORByteString( false, new byte[] { 5, 0, 0 } );
		CBORByteString b6 = new CBORByteString( false, new byte[] { 6, 0, 0 } );

		index.put( b1 );
		assertThat( index.get0( 0 ) ).isEqualTo( b1 );

		index.put( b2 );
		assertThat( index.get0( 1 ) ).isEqualTo( b1 );
		assertThat( index.get0( 0 ) ).isEqualTo( b2 );

		index.put( b3 );
		assertThat( index.get0( 2 ) ).isEqualTo( b1 );
		assertThat( index.get0( 1 ) ).isEqualTo( b2 );
		assertThat( index.get0( 0 ) ).isEqualTo( b3 );

		index.put( b4 );
		assertThat( index.get0( 3 ) ).isEqualTo( b1 );
		assertThat( index.get0( 2 ) ).isEqualTo( b2 );
		assertThat( index.get0( 1 ) ).isEqualTo( b3 );
		assertThat( index.get0( 0 ) ).isEqualTo( b4 );

		index.put( b5 );
		assertThat( index.get0( 3 ) ).isEqualTo( b2 );
		assertThat( index.get0( 2 ) ).isEqualTo( b3 );
		assertThat( index.get0( 1 ) ).isEqualTo( b4 );
		assertThat( index.get0( 0 ) ).isEqualTo( b5 );

		index.put( b6 );
		assertThat( index.get0( 3 ) ).isEqualTo( b3 );
		assertThat( index.get0( 2 ) ).isEqualTo( b4 );
		assertThat( index.get0( 1 ) ).isEqualTo( b5 );
		assertThat( index.get0( 0 ) ).isEqualTo( b6 );

		index.put( b3 ); // The one that would fall off the window
		assertThat( index.get0( 3 ) ).isEqualTo( b4 );
		assertThat( index.get0( 2 ) ).isEqualTo( b5 );
		assertThat( index.get0( 1 ) ).isEqualTo( b6 );
		assertThat( index.get0( 0 ) ).isEqualTo( b3 );

		CBORByteString b7 = new CBORByteString( false, new byte[] { 1, 0, 0 } );
		index.put( b7 ); // Not in the index
		assertThat( index.get0( 3 ) ).isEqualTo( b5 );
		assertThat( index.get0( 2 ) ).isEqualTo( b6 );
		assertThat( index.get0( 1 ) ).isEqualTo( b3 );
		assertThat( index.get0( 0 ) ).isEqualTo( b1 );

		CBORByteString b8 = new CBORByteString( false, new byte[] { 6, 0, 0 } );
		index.put( b8 ); // Already in the index
		assertThat( index.get0( 3 ) ).isEqualTo( b5 );
		assertThat( index.get0( 2 ) ).isEqualTo( b3 );
		assertThat( index.get0( 1 ) ).isEqualTo( b1 );
		assertThat( index.get0( 0 ) ).isEqualTo( b8 );
	}
}
