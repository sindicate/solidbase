package solidbase.http;

public class RequestContext
{
	protected Request request;
	protected Response reponse;
	protected ApplicationContext applicationContext;

	public RequestContext( Request request, Response response, ApplicationContext applicationContext )
	{
		this.request = request;
		this.reponse = response;
		this.applicationContext = applicationContext;
	}

	public Request getRequest()
	{
		return this.request;
	}

	public Response getResponse()
	{
		return this.reponse;
	}

//	public void callJsp( String jsp )
//	{
//		this.applicationContext.callJsp( jsp, this );
//	}

	public ApplicationContext getApplication()
	{
		return this.applicationContext;
	}
}
