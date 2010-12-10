
var walker = function( element, f )
{
	var node = element.firstElementChild
	while( node !== null )
	{
		if( f( node ) )
			walker( node, f )
		node = node.nextElementSibling
	}
};

function getAsync( url, callback )
{
	var request = new XMLHttpRequest();

	request.onreadystatechange = function()
	{
		if( request.readyState == 4 )
		{
			if( request.status == 200 )
			{
				callback( request.responseText );
				return;
			}

			// Error
			throw "request returned HTTP Status " + request.status;
		}
	};

	request.open( "GET", url, true );
	request.send();
}
