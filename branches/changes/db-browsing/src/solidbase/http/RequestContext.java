package solidbase.http;

public class RequestContext
{
	private Request request;
	private Response reponse;

	public RequestContext( Request request, Response response )
	{
		this.request = request;
		this.reponse = response;
	}

	public Request getRequest()
	{
		return this.request;
	}

	public Response getResponse()
	{
		return this.reponse;
	}

	public void callJsp( String jsp )
	{
		JspManager.call( jsp, this );
	}
}
