package solidstack.cbor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.Test;

import solidstack.cbor.IndexTree.Node;


public class IndexTreeTests
{
	@Test
	public void test1()
	{
		int count = 1000;

		IndexTree tree = new IndexTree();

		List<Node> nodes = new ArrayList<>();

		for( int i = 0; i < count; i++ )
		{
			System.out.println( i );
			Node node = tree.add();
			nodes.add( node );
			int j = 0;
			for( Node n : nodes )
				assertThat( n.getIndex() ).isEqualTo( j++ );
		}

		tree.remove( tree.root );
		assertThat( tree.size() ).isEqualTo( count - 1 );

		Node n10 = tree.get( 10 );
		tree.remove( 0 );
		assertThat( tree.size() ).isEqualTo( count - 2 );
		assertThat( n10.getIndex() ).isEqualTo( 9 );

		int len = tree.size();
		Random rnd = new Random( 0 ); // Always generates the same sequence
		for( int i = 0; i < len; i++ )
		{
			System.out.println( i );
			int in = rnd.nextInt( tree.size() );
			int rem = rnd.nextInt( tree.size() );
			Node n = tree.get( in );
			tree.remove( rem );
			if( in > rem )
				assertThat( n.getIndex() ).isEqualTo( in - 1 );
			else if( in < rem )
				assertThat( n.getIndex() ).isEqualTo( in );
		}

		assertThat( tree.size() ).isEqualTo( 0 );
		assertThat( tree.isEmpty() ).isEqualTo( true );
	}
}
