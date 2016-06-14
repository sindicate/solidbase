package solidstack.cbor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.Test;

import solidstack.cbor.TreeIndex.Node;


public class TreeIndexTests
{
	@Test
	public void test1()
	{
		int count = 1000;

		TreeIndex tree = new TreeIndex();

		List<Node> nodes = new ArrayList<Node>();

		for( int i = 0; i < count; i++ )
		{
			System.out.println( i );
			Node node = tree.addLast();
			nodes.add( node );
			int j = 0;
			for( Node n : nodes )
				assertThat( n.index() ).isEqualTo( j++ );
		}

		tree.remove( tree.root );
		assertThat( tree.size() ).isEqualTo( count - 1 );

		Node n10 = tree.get( 10 );
		tree.remove( 0 );
		assertThat( tree.size() ).isEqualTo( count - 2 );
		assertThat( n10.index() ).isEqualTo( 9 );

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
				assertThat( n.index() ).isEqualTo( in - 1 );
			else if( in < rem )
				assertThat( n.index() ).isEqualTo( in );
		}

		assertThat( tree.size() ).isEqualTo( 0 );
		assertThat( tree.isEmpty() ).isEqualTo( true );
	}

	@Test
	public void test2()
	{
		int count = 1000;

		TreeIndex tree = new TreeIndex();

		List<String> index = new ArrayList<String>();
		List<Node<String>> index2 = new ArrayList<Node<String>>();

		Random rnd = new Random( 0 ); // Always generates the same sequence
		for( int i = 0; i < count; i++ )
		{
			int in = rnd.nextInt( 100 );
			String s = Integer.toString( in );
			System.out.println( i + ", " + in );

			Node node = tree.addFirst( s );
			int ii = index.indexOf( s );
			if( ii >= 0 )
			{
				index.remove( ii );
				index2.remove( ii );
			}
			index.add( 0, s );
			index2.add( 0, node );

			int len = index.size();
			for( int j = 0; j < len; j++ )
			{
				s = index.get( j );
				String s2 = index2.get( j ).data;
				assertThat( s2 ).isEqualTo( s );
			}
		}
	}
}
