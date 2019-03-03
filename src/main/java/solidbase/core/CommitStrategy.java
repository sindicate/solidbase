package solidbase.core;

/**
 * The commit strategy to use.
 */
public enum CommitStrategy
{
	/**
	 * Enable auto commit on the JDBC connection.
	 */
	AUTOCOMMIT,

	/**
	 * Disable auto commit on the JDBC connection. Note that during an upgrade, an explicit commit is done after every
	 * command sent to the database.
	 */
	TRANSACTIONAL

}
