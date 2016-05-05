package solidstack.cbor;

import java.io.OutputStream;

import solidstack.json.JSONArray;

public class CBOROutputStream
{
	public CBOROutputStream( OutputStream out )
	{
		// TODO Auto-generated constructor stub
	}

	public void write( JSONArray array )
	{
		for( Object object : array )
		{
			throw new UnsupportedOperationException( "Writing not supported for: " + object.getClass().getName() );
		}
	}

	public void close()
	{
		// TODO Auto-generated method stub

	}
}
