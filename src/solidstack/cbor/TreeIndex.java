package solidstack.cbor;

import java.util.NoSuchElementException;


/**
 * Maintains an index of every object added. Removal of an object changes all the indexes of the objects behind it. Adding
 * an object in front also changes all the indexes of the object behind it.
 *
 * @param <T> The type of the objects stored in the nodes of this tree.
 */
public class TreeIndex<T>
{
	Node<T> root;


	/**
	 * Constructs a new {@link TreeIndex}.
	 */
	public TreeIndex()
	{
	}

	/**
	 * @return The number of items in the tree.
	 */
	public int size()
	{
		if( this.root != null )
			return this.root.size;
		return 0;
	}

	/**
	 * @return True if the tree is empty, false otherwise.
	 */
	public boolean isEmpty()
	{
		return this.root == null;
	}

	/**
	 * Removes all the items from the tree.
	 */
	public void clear()
	{
		this.root = null;
	}

	/**
	 * Adds a node at the end.
	 *
	 * @return The node that has been added.
	 */
	public Node<T> addLast()
	{
		Node<T> parent = this.root;
		if( parent == null )
			return this.root = new Node<T>( null );

		Node<T> t;
		while( ( t = parent.right ) != null )
			parent = t;
		Node<T> result = parent.right = new Node<T>( parent );

		while( parent != null )
		{
			parent.size++;
			parent = parent.parent;
		}

		fixAfterInsertion( result );
		return result;
	}

	/**
	 * Adds an object at the end.
	 *
	 * @param data The object to add.
	 * @return The node that has been added.
	 */
	public Node<T> addLast( T data )
	{
		Node<T> result = addLast();
		result.data = data;
		return result;
	}

	/**
	 * Adds a node to the front.
	 *
	 * @return The node that has been added.
	 */
	public Node<T> addFirst()
	{
		Node<T> parent = this.root;
		if( parent == null )
			return this.root = new Node<T>( null );

		Node<T> t;
		while( ( t = parent.left ) != null )
			parent = t;
		Node<T> result = parent.left = new Node<T>( parent );

		while( parent != null )
		{
			parent.size++;
			parent = parent.parent;
		}

		fixAfterInsertion( result );
		return result;
	}

	/**
	 * Adds an object to the front.
	 *
	 * @param data The object to add.
	 * @return The node that has been added.
	 */
	public Node<T> addFirst( T data )
	{
		Node<T> result = addFirst();
		result.data = data;
		return result;
	}

	/**
	 * @param index The index of the node to retrieve.
	 * @return The node with the given index.
	 */
	public Node<T> get( int index )
	{
		if( index < 0 || index >= size() )
			throw new IndexOutOfBoundsException( "index: " + index + ", size: " + size() );

		Node<T> n = this.root;
		int i = size( n.left );
		while( i != index )
			if( index < i )
			{
				n = n.left;
				i -= 1 + size( n.right );
			}
			else
			{
				n = n.right;
				i += 1 + size( n.left );
			}
		return n;
	}

	/**
	 * Removes the node at the front.
	 *
	 * @return The node that has been removed.
	 */
	public Node<T> removeFirst()
	{
		if( this.root == null )
			throw new NoSuchElementException();
		return remove( 0 );
	}

	/**
	 * Removes the node at the end.
	 *
	 * @return The node that has been removed.
	 */
	public Node<T> removeLast()
	{
		if( this.root == null )
			throw new NoSuchElementException();
		return remove( this.root.size - 1 );
	}

	/**
	 * Removes the node with the given index.
	 *
	 * @param index The index of the node to remove.
	 * @return The node that has been removed.
	 */
	public Node<T> remove( int index )
	{
		Node<T> result = get( index );
		remove( result );
		return result;
	}

	/**
	 * Remove the given node.
	 *
	 * @param node The node to remove.
	 */
	public void remove( Node<T> node )
	{
		if( node.left != null && node.right != null )
			switchEntries( node, successor( node ) );

		Node<T> parent = node.parent;
		while( parent != null )
		{
			parent.size --;
			parent = parent.parent;
		}

		Node<T> replacement = node.left != null ? node.left : node.right;
		if( replacement != null )
		{
			replacement.parent = node.parent;
			if( node.parent == null )
				this.root = replacement;
			else if( node == node.parent.left )
				node.parent.left = replacement;
			else
				node.parent.right = replacement;

			if( node.color == BLACK )
				fixAfterDeletion( replacement );
		}
		else if( node.parent == null )
		{
			this.root = null;
		}
		else
		{
			if( node.color == BLACK )
			{
				node.size = 0;
				fixAfterDeletion( node );
			}

			parent = node.parent;
			if( parent != null )
				if( node == node.parent.left )
					node.parent.left = null;
				else if( node == node.parent.right )
					node.parent.right = null;
		}
	}

	// Red-black mechanics

	private static final boolean RED = false;
	private static final boolean BLACK = true;

	private void switchEntries( Node<T> e1, Node<T> e2 )
	{
		Node<T> e1Parent = e1.parent;
		boolean e1LeftChild = e1Parent != null && e1Parent.left == e1;
		Node<T> e1Left = e1.left;
		Node<T> e1Right = e1.right;
		boolean e1Color = e1.color;
		int e1Size = e1.size;

		Node<T> e2Parent = e2.parent;
		boolean e2LeftChild = e2Parent != null && e2Parent.left == e2;
		Node<T> e2Left = e2.left;
		Node<T> e2Right = e2.right;
		boolean e2Color = e2.color;
		int e2Size = e2.size;

		e2.parent = e1Parent;
		e2.left = e1Left;
		e2.right = e1Right;
		e2.color = e1Color;
		e2.size = e1Size;

		e1.parent = e2Parent;
		e1.left = e2Left;
		e1.right = e2Right;
		e1.color = e2Color;
		e1.size = e2Size;

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
	static <T> Node<T> successor( Node<T> t )
	{
		if( t == null )
			return null;

		if( t.right != null )
		{
			Node<T> p = t.right;
			while( p.left != null )
				p = p.left;
			return p;
		}

		Node<T> p = t.parent;
		Node<T> ch = t;
		while( p != null && ch == p.right )
		{
			ch = p;
			p = p.parent;
		}
		return p;
	}

	private static <T> boolean colorOf( Node<T> p )
	{
		return p == null ? BLACK : p.color;
	}

	private static <T> Node<T> parentOf( Node<T> p )
	{
		return p == null ? null : p.parent;
	}

	private static <T> void setColor( Node<T> p, boolean c )
	{
		if( p != null )
			p.color = c;
	}

	private static <T> Node<T> leftOf( Node<T> p )
	{
		return p == null ? null : p.left;
	}

	private static <T> Node<T> rightOf( Node<T> p )
	{
		return p == null ? null : p.right;
	}

	// p, left and right, and leftofright
	// right komt aan top
	// p komt links van right
	// leftofright komt rechts van p
	private void rotateLeft( Node<T> p )
	{
		if( p != null )
		{
			Node<T> r = p.right;
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

			p.size = size( p.left ) + size( p.right ) + 1;
			r.size = p.size + size( r.right ) + 1;
		}
	}

	static <T> int size( Node<T> node )
	{
		if( node != null )
			return node.size;
		return 0;
	}

	private void rotateRight( Node<T> p )
	{
		if( p != null )
		{
			Node<T> l = p.left;
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

			p.size = size( p.left ) + size( p.right ) + 1;
			l.size = size( l.left ) + p.size + 1;
		}
	}

	private void fixAfterInsertion( Node<T> x )
	{
		x.color = RED;

		while( x != null && x != this.root && x.parent.color == RED )
		{
			if( parentOf( x ) == leftOf( parentOf( parentOf( x ) ) ) )
			{
				Node<T> y = rightOf( parentOf( parentOf( x ) ) );
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
				Node<T> y = leftOf( parentOf( parentOf( x ) ) );
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

	private void fixAfterDeletion( Node<T> x )
	{
		while( x != this.root && colorOf( x ) == BLACK )
		{
			if( x == leftOf( parentOf( x ) ) )
			{
				Node<T> sib = rightOf( parentOf( x ) );

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
				Node<T> sib = leftOf( parentOf( x ) );

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

	static class Node<T>
	{
		Node<T> left, right, parent;
		boolean color = RED;
		int size;
		T data;

		Node( Node<T> parent )
		{
			this.parent = parent;
			this.size = 1;
		}

		public int index()
		{
			int index = size( this.left );
			Node<T> n = this;
			Node<T> parent = this.parent;
			while( parent != null )
			{
				if( parent.right == n )
					index += size( parent.left ) + 1;
				n = parent;
				parent = n.parent;
			}
			return index;
		}

		public T data()
		{
			return this.data;
		}

		public void setData( T data )
		{
			this.data = data;
		}
	}
}
