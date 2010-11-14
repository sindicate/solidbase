
var walker = function( element, f )
{
	var node = element.firstChild
	while( node !== null )
	{
		if( node instanceof Element )
		{
			f( node )
			walker( node, f )
		}
		node = node.nextSibling
	}
};

