package solidbase.core.plugins;

public interface RecordSource
{
	Column[] getColumns();
	void setOutput( DataProcessor output );
	Object[] getCurrentValues();
}
