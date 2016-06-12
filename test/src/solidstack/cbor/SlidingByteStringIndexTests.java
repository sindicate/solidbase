package solidstack.cbor;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;


public class SlidingByteStringIndexTests
{
	@Test
	public void test1()
	{
		SlidingByteStringIndex index = new SlidingByteStringIndex( 4 );
		CBORByteString b1 = new CBORByteString( false, new byte[] { 1 } );
		CBORByteString b2 = new CBORByteString( false, new byte[] { 2 } );
		CBORByteString b3 = new CBORByteString( false, new byte[] { 3 } );
		CBORByteString b4 = new CBORByteString( false, new byte[] { 4 } );
		CBORByteString b5 = new CBORByteString( false, new byte[] { 5 } );
		CBORByteString b6 = new CBORByteString( false, new byte[] { 6 } );

		index.put( b1 );
		assertThat( index.get( b1 ) ).isEqualTo( 0 );

		index.put( b2 );
		assertThat( index.get( b1 ) ).isEqualTo( 1 );
		assertThat( index.get( b2 ) ).isEqualTo( 0 );

		index.put( b3 );
		assertThat( index.get( b1 ) ).isEqualTo( 2 );
		assertThat( index.get( b2 ) ).isEqualTo( 1 );
		assertThat( index.get( b3 ) ).isEqualTo( 0 );

		index.put( b4 );
		assertThat( index.get( b1 ) ).isEqualTo( 3 );
		assertThat( index.get( b2 ) ).isEqualTo( 2 );
		assertThat( index.get( b3 ) ).isEqualTo( 1 );
		assertThat( index.get( b4 ) ).isEqualTo( 0 );

		index.put( b5 );
		assertThat( index.get( b1 ) ).isNull();
		assertThat( index.get( b2 ) ).isEqualTo( 3 );
		assertThat( index.get( b3 ) ).isEqualTo( 2 );
		assertThat( index.get( b4 ) ).isEqualTo( 1 );
		assertThat( index.get( b5 ) ).isEqualTo( 0 );

		index.put( b6 );
		assertThat( index.get( b1 ) ).isNull();
		assertThat( index.get( b2 ) ).isNull();
		assertThat( index.get( b3 ) ).isEqualTo( 3 );
		assertThat( index.get( b4 ) ).isEqualTo( 2 );
		assertThat( index.get( b5 ) ).isEqualTo( 1 );
		assertThat( index.get( b6 ) ).isEqualTo( 0 );

		index.put( b3 ); // The one that would fall off the window
		assertThat( index.get( b1 ) ).isNull();
		assertThat( index.get( b2 ) ).isNull();
		assertThat( index.get( b4 ) ).isEqualTo( 3 );
		assertThat( index.get( b5 ) ).isEqualTo( 2 );
		assertThat( index.get( b6 ) ).isEqualTo( 1 );
		assertThat( index.get( b3 ) ).isEqualTo( 0 );

		CBORByteString b7 = new CBORByteString( false, new byte[] { 1 } );
		index.put( b7 ); // Not in the index
		assertThat( index.get( b1 ) ).isEqualTo( 0 );
		assertThat( index.get( b2 ) ).isNull();
		assertThat( index.get( b4 ) ).isNull();
		assertThat( index.get( b5 ) ).isEqualTo( 3 );
		assertThat( index.get( b6 ) ).isEqualTo( 2 );
		assertThat( index.get( b3 ) ).isEqualTo( 1 );
		assertThat( index.get( b7 ) ).isEqualTo( 0 );

		// TODO The old one is removed from the index, so we have holes, so the index will not contain as much as it can, but otherwise a performance problem?
		CBORByteString b8 = new CBORByteString( false, new byte[] { 6 } );
		index.put( b8 ); // Already in the index
		assertThat( index.get( b1 ) ).isEqualTo( 1 );
		assertThat( index.get( b2 ) ).isNull();
		assertThat( index.get( b4 ) ).isNull();
		assertThat( index.get( b5 ) ).isNull();
		assertThat( index.get( b6 ) ).isEqualTo( 0 );
		assertThat( index.get( b3 ) ).isEqualTo( 2 );
		assertThat( index.get( b7 ) ).isEqualTo( 1 );
		assertThat( index.get( b8 ) ).isEqualTo( 0 );
	}
}
