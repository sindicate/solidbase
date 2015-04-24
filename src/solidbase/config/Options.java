/*--
 * Copyright 2010 René M. de Bloois
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solidbase.config;


/**
 * Options from the command line.
 * 
 * @author René M. de Bloois
 */
public class Options
{
	/**
	 * Be extra verbose.
	 */
	public boolean verbose;

	/**
	 * Export historical upgrade results to an XML file.
	 */
	public boolean dumplog;

	/**
	 * The JDBC driver class name.
	 */
	public String driver;

	/**
	 * The URL for the database.
	 */
	public String url;

	/**
	 * The default user name to connect to the database.
	 */
	public String username;

	/**
	 * The password of the default user.
	 */
	public String password;

	/**
	 * The target to upgrade to.
	 */
	public String target;

	/**
	 * The file containing the database upgrades.
	 */
	public String upgradefile;

	/**
	 * A file containing SQL to be executed.
	 */
	public String sqlfile;

	/**
	 * A properties file to use.
	 */
	public String config;

	/**
	 * Allow downgrades to reach the target.
	 */
	public boolean downgradeallowed;

	/**
	 * Brings up the help.
	 */
	public boolean help;

	/**
	 * @param verbose Be extra verbose.
	 * @param dumplog Export historical upgrade results to an XML file.
	 * @param driver The JDBC driver class name.
	 * @param url The URL for the database.
	 * @param username The default user name to connect to the database.
	 * @param password The password of the default user.
	 * @param target The target to upgrade to.
	 * @param upgradefile The file containing the database upgrades.
	 * @param sqlfile A file containing SQL to be executed.
	 * @param config A properties file to use.
	 * @param downgradeallowed Allow downgrades to reach the target.
	 * @param help Brings up the help.
	 */
	public Options( boolean verbose, boolean dumplog, String driver, String url, String username, String password,
			String target, String upgradefile, String sqlfile, String config, boolean downgradeallowed, boolean help )
	{
		this.verbose = verbose;
		this.dumplog = dumplog;
		this.downgradeallowed = downgradeallowed;
		this.help = help;
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
		this.target = target;
		this.upgradefile = upgradefile;
		this.sqlfile = sqlfile;
		this.config = config;
	}
}
