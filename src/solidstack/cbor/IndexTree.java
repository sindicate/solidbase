package solidstack.cbor;

import solidbase.core.FatalException;

/**
 * This is going to be a tree which adds items at the right and removes from anywhere and keeps a count of nodes at the right in every node.
 *
 */
public class IndexTree
{
	Node root;

	static class Node
	{
		int size;
		int leftSize;
		Node left, right;
		Node parent;
		boolean color = RED;

		Node( Node parent )
		{
			this.parent = parent;
			this.size = 1;
		}

		public int getIndex()
		{
			int index = this.leftSize;
			Node n = this;
			Node parent = this.parent;
			while( parent != null )
			{
				if( parent.right == n )
					index += parent.leftSize + 1;
				n = parent;
				parent = n.parent;
			}
			return index;
		}
	}

	public IndexTree()
	{
	}

	public int size()
	{
		if( this.root != null )
			return this.root.size;
		return 0;
	}

	public boolean isEmpty()
	{
		return this.root == null;
	}

	public void clear()
	{
		this.root = null;
	}

	public Node add()
	{
		Node parent = this.root;
		if( parent == null )
		{
			// Was empty
			return this.root = new Node( null );
		}

		Node t;
		while( ( t = parent.right ) != null )
			parent = t;

		Node result = parent.right = new Node( parent );

		parent = result.parent;
		while( parent != null )
		{
			parent.size++;
			parent = parent.parent;
		}

		fixAfterInsertion( result );

		return result;
	}

	public Node get( int index )
	{
		if( index < 0 || index >= size() )
			throw new IndexOutOfBoundsException( "index: " + index + ", size: " + size() );
		Node n = this.root;
		int i = n.leftSize;
		do
		{
			if( index == i )
				return n;
			if( index < i )
			{
				n = n.left;
				if( n != null )
					i = i - ( n.size - n.leftSize );
			}
			else
			{
				n = n.right;
				if( n != null )
					i = i + 1 + n.leftSize;
			}
		}
		while( n != null );
		throw new FatalException( "Unexpected" );
	}

	public void remove( int index )
	{
		remove( get( index ) );
	}

	public void remove( Node p )
	{
		if( p.left != null && p.right != null )
			switchEntries( p, successor( p ) );

		// Start fixup at replacement node, if it exists.
		Node replacement = p.left != null ? p.left : p.right;

		if( replacement != null )
		{
			// Link replacement to parent
			replacement.parent = p.parent;
			if( p.parent == null )
				this.root = replacement;
			else if( p == p.parent.left )
				p.parent.left = replacement;
			else
				p.parent.right = replacement;

			// Null out links so they are OK to use by fixAfterDeletion.
			p.left = p.right = p.parent = null;

			Node parent = replacement.parent;
			while( parent != null )
			{
				parent.leftSize = parent.left.size;
				parent.size = 1 + parent.leftSize + parent.right.size;
				parent = parent.parent;
			}

			// Fix replacement
			if( p.color == BLACK )
				fixAfterDeletion( replacement );
		}
		else if( p.parent == null )
		{
			this.root = null;
		}
		else
		{
			if( p.color == BLACK )
				fixAfterDeletion( p );

			if( p.parent != null )
			{
				Node parent = p.parent;

				if( p == p.parent.left )
					p.parent.left = null;
				else if( p == p.parent.right )
					p.parent.right = null;
				p.parent = null;

				while( parent != null )
				{
					parent.leftSize = size( parent.left );
					parent.size = 1 + parent.leftSize + size( parent.right );
					parent = parent.parent;
				}
			}
		}
	}

	// Red-black mechanics

	private static final boolean RED = false;
	private static final boolean BLACK = true;

	private void switchEntries( Node e1, Node e2 )
	{
		Node e1Parent = e1.parent;
		boolean e1LeftChild = e1Parent != null && e1Parent.left == e1;
		Node e1Left = e1.left;
		Node e1Right = e1.right;
		boolean e1Color = e1.color;
		int e1Size = e1.size;
		int e1LeftSize = e1.leftSize;

		Node e2Parent = e2.parent;
		boolean e2LeftChild = e2Parent != null && e2Parent.left == e2;
		Node e2Left = e2.left;
		Node e2Right = e2.right;
		boolean e2Color = e2.color;
		int e2Size = e2.size;
		int e2LeftSize = e2.leftSize;

		e2.parent = e1Parent;
		e2.left = e1Left;
		e2.right = e1Right;
		e2.color = e1Color;
		e2.size = e1Size;
		e2.leftSize = e1LeftSize;

		e1.parent = e2Parent;
		e1.left = e2Left;
		e1.right = e2Right;
		e1.color = e2Color;
		e1.size = e2Size;
		e1.leftSize = e2LeftSize;

		if( e1Parent == e2 )
		{
			e2.parent = e1Parent = e1;
			if( e1LeftChild )
				e1.left = e2Left = e2;
			else
				e1.right = e2Right = e2;
		}
		else if( e2Parent == e1 )
		{
			e1.parent = e2Parent = e2;
			if( e2LeftChild )
				e2.left = e1Left = e1;
			else
				e2.right = e1Right = e1;
		}

		if( e1Parent == null )
			this.root = e2;
		else if( e1LeftChild )
			e1Parent.left = e2;
		else
			e1Parent.right = e2;
		if( e1Left != null )
			e1Left.parent = e2;
		if( e1Right != null )
			e1Right.parent = e2;

		if( e2Parent == null )
			this.root = e1;
		else if( e2LeftChild )
			e2Parent.left = e1;
		else
			e2Parent.right = e1;
		if( e2Left != null )
			e2Left.parent = e1;
		if( e2Right != null )
			e2Right.parent = e1;
	}

    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
	static Node successor( Node t )
	{
		if( t == null )
			return null;

		if( t.right != null )
		{
			Node p = t.right;
			while( p.left != null )
				p = p.left;
			return p;
		}

		Node p = t.parent;
		Node ch = t;
		while( p != null && ch == p.right )
		{
			ch = p;
			p = p.parent;
		}
		return p;
	}

	private static boolean colorOf( Node p )
	{
		return p == null ? BLACK : p.color;
	}

	private static Node parentOf( Node p )
	{
		return p == null ? null : p.parent;
	}

	private static void setColor( Node p, boolean c )
	{
		if( p != null )
			p.color = c;
	}

	private static Node leftOf( Node p )
	{
		return p == null ? null : p.left;
	}

	private static Node rightOf( Node p )
	{
		return p == null ? null : p.right;
	}

	// p, left and right, and leftofright
	// right komt aan top
	// p komt links van right
	// leftofright komt rechts van p
	private void rotateLeft( Node p )
	{
		if( p != null )
		{
			Node r = p.right;
			p.right = r.left;
			if( r.left != null )
				r.left.parent = p;
			r.parent = p.parent;
			if( p.parent == null )
				this.root = r;
			else if( p.parent.left == p )
				p.parent.left = r;
			else
				p.parent.right = r;
			r.left = p;
			p.parent = r;

//			p.leftSize = p.left.size;
			p.size = p.leftSize + size( p.right ) + 1;

			r.leftSize = p.size;
			r.size = r.leftSize + size( r.right ) + 1;
		}
	}

	static private int size( Node node )
	{
		if( node != null )
			return node.size;
		return 0;
	}

	private void rotateRight( Node p )
	{
		if( p != null )
		{
			Node l = p.left;
			p.left = l.right;
			if( l.right != null )
				l.right.parent = p;
			l.parent = p.parent;
			if( p.parent == null )
				this.root = l;
			else if( p.parent.right == p )
				p.parent.right = l;
			else
				p.parent.left = l;
			l.right = p;
			p.parent = l;

			p.leftSize = size( p.left );
			p.size = p.leftSize + size( p.right ) + 1;

//			l.leftSize = p.size;
			l.size = l.leftSize + l.right.size + 1;
		}
	}

	private void fixAfterInsertion( Node x )
	{
		x.color = RED;

		while( x != null && x != this.root && x.parent.color == RED )
		{
			if( parentOf( x ) == leftOf( parentOf( parentOf( x ) ) ) )
			{
				Node y = rightOf( parentOf( parentOf( x ) ) );
				if( colorOf( y ) == RED )
				{
					setColor( parentOf( x ), BLACK );
					setColor( y, BLACK );
					setColor( parentOf( parentOf( x ) ), RED );
					x = parentOf( parentOf( x ) );
				}
				else
				{
					if( x == rightOf( parentOf( x ) ) )
					{
						x = parentOf( x );
						rotateLeft( x );
					}
					setColor( parentOf( x ), BLACK );
					setColor( parentOf( parentOf( x ) ), RED );
					rotateRight( parentOf( parentOf( x ) ) );
				}
			}
			else
			{
				Node y = leftOf( parentOf( parentOf( x ) ) );
				if( colorOf( y ) == RED )
				{
					setColor( parentOf( x ), BLACK );
					setColor( y, BLACK );
					setColor( parentOf( parentOf( x ) ), RED );
					x = parentOf( parentOf( x ) );
				}
				else
				{
					if( x == leftOf( parentOf( x ) ) )
					{
						x = parentOf( x );
						rotateRight( x );
					}
					setColor( parentOf( x ), BLACK );
					setColor( parentOf( parentOf( x ) ), RED );
					rotateLeft( parentOf( parentOf( x ) ) );
				}
			}
		}
		this.root.color = BLACK;
	}

	private void fixAfterDeletion( Node x )
	{
		while( x != this.root && colorOf( x ) == BLACK )
		{
			if( x == leftOf( parentOf( x ) ) )
			{
				Node sib = rightOf( parentOf( x ) );

				if( colorOf( sib ) == RED )
				{
					setColor( sib, BLACK );
					setColor( parentOf( x ), RED );
					rotateLeft( parentOf( x ) );
					sib = rightOf( parentOf( x ) );
				}

				if( colorOf( leftOf( sib ) ) == BLACK && colorOf( rightOf( sib ) ) == BLACK )
				{
					setColor( sib, RED );
					x = parentOf( x );
				}
				else
				{
					if( colorOf( rightOf( sib ) ) == BLACK )
					{
						setColor( leftOf( sib ), BLACK );
						setColor( sib, RED );
						rotateRight( sib );
						sib = rightOf( parentOf( x ) );
					}
					setColor( sib, colorOf( parentOf( x ) ) );
					setColor( parentOf( x ), BLACK );
					setColor( rightOf( sib ), BLACK );
					rotateLeft( parentOf( x ) );
					x = this.root;
				}
			}
			else
			{ // symmetric
				Node sib = leftOf( parentOf( x ) );

				if( colorOf( sib ) == RED )
				{
					setColor( sib, BLACK );
					setColor( parentOf( x ), RED );
					rotateRight( parentOf( x ) );
					sib = leftOf( parentOf( x ) );
				}

				if( colorOf( rightOf( sib ) ) == BLACK && colorOf( leftOf( sib ) ) == BLACK )
				{
					setColor( sib, RED );
					x = parentOf( x );
				}
				else
				{
					if( colorOf( leftOf( sib ) ) == BLACK )
					{
						setColor( rightOf( sib ), BLACK );
						setColor( sib, RED );
						rotateLeft( sib );
						sib = leftOf( parentOf( x ) );
					}
					setColor( sib, colorOf( parentOf( x ) ) );
					setColor( parentOf( x ), BLACK );
					setColor( leftOf( sib ), BLACK );
					rotateRight( parentOf( x ) );
					x = this.root;
				}
			}
		}

		setColor( x, BLACK );
	}
}
