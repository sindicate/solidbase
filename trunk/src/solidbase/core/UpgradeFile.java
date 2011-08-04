/*--
 * Copyright 2006 René M. de Bloois
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

package solidbase.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import solidbase.core.UpgradeSegment.Type;
import solidbase.util.Assert;
import solidbase.util.RandomAccessLineReader;


/**
 * This class manages the upgrade file contents and the paths between versions.
 *
 * @author René M. de Bloois
 * @since Apr 1, 2006 7:18:27 PM
 */
public class UpgradeFile
{
	static private final Pattern PATCH_DEFINITION_MARKER_PATTERN = Pattern.compile( "(SETUP|UPGRADE|SWITCH|DOWNGRADE|INIT|PATCH|BRANCH|RETURN)[ \t]+.*", Pattern.CASE_INSENSITIVE );
	static private final Pattern PATCH_DEFINITION_PATTERN = Pattern.compile( "(SETUP|UPGRADE|SWITCH|DOWNGRADE|INIT|PATCH|BRANCH|RETURN)([ \t]+OPEN)?[ \t]+\"([^\"]*)\"[ \t]+-->[ \t]+\"([^\"]+)\"([ \t]*//.*)?", Pattern.CASE_INSENSITIVE );
	static private final String PATCH_DEFINITION_SYNTAX_ERROR = "Line should match the following syntax: (SETUP|UPGRADE|SWITCH|DOWNGRADE) [OPEN] \"...\" --> \"...\"";

	static private final Pattern CONTROL_TABLES_PATTERN = Pattern.compile( "VERSION\\s+TABLE\\s+(\\S+)\\s+LOG\\s+TABLE\\s+(\\S+)", Pattern.CASE_INSENSITIVE );

	static private final Pattern PATCH_START_MARKER_PATTERN = Pattern.compile( "--\\*[ \t]*(SETUP|UPGRADE|SWITCH|DOWNGRADE|INIT|PATCH|BRANCH|RETURN).*", Pattern.CASE_INSENSITIVE );
	static final Pattern PATCH_START_PATTERN = Pattern.compile( "(SETUP|UPGRADE|SWITCH|DOWNGRADE|INIT|PATCH|BRANCH|RETURN)[ \t]+\"([^\"]*)\"[ \t]-->[ \t]+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE );

	static final Pattern PATCH_END_PATTERN = Pattern.compile( "/(SETUP|UPGRADE|SWITCH|DOWNGRADE|INIT|PATCH|BRANCH|RETURN) *", Pattern.CASE_INSENSITIVE );

//	static private final Pattern INITIALIZATION_TRIGGER = Pattern.compile( "--\\*\\s*INITIALIZATION\\s*", Pattern.CASE_INSENSITIVE );
//	static private final Pattern INITIALIZATION_END_PATTERN = Pattern.compile( "--\\*\\s*/INITIALIZATION\\s*", Pattern.CASE_INSENSITIVE );

//	static private final Pattern INIT_CONNECTION_TRIGGER = Pattern.compile( "--\\*\\s*INIT\\s+CONNECTION.*", Pattern.CASE_INSENSITIVE );
//	static private final Pattern INIT_CONNECTION_PARSER = Pattern.compile( "--\\*\\s*INIT\\s+CONNECTION(?:\\s+(\\S+)(?:\\s+USER\\s+(\\S+))?)?\\s*", Pattern.CASE_INSENSITIVE );
//	static private final String INIT_CONNECTION_SYNTAX = "INIT CONNECTION <connectionname> [USER <username>]";
//	static private final Pattern INIT_CONNECTION_END_PATTERN = Pattern.compile( "--\\*\\s*/INIT\\s+CONNECTION\\s*", Pattern.CASE_INSENSITIVE );

	static private final String MARKER_SYNTAX_ERROR = "Line should match the following syntax: (SETUP|UPGRADE|SWITCH|DOWNGRADE) \"...\" --> \"...\"" /* or INIT CONNECTION <name> */;

	/**
	 * The upgrade file.
	 */
	protected RandomAccessLineReader file;

	/**
	 * The default delimiters.
	 */
	protected Delimiter[] defaultDelimiters = SQLSource.DEFAULT_DELIMITERS;

	/**
	 * All normal segments in a map indexed by source version.
	 */
	protected Map< String, Collection< UpgradeSegment > > segments = new HashMap< String, Collection< UpgradeSegment > >();

	/**
	 * Contains all known versions from the upgrade file.
	 */
	protected Set< String > versions = new HashSet< String >();

	/**
	 * All setup segments in a map indexed by source version.
	 */
	protected Map< String, UpgradeSegment > setups = new HashMap< String, UpgradeSegment >();

//	/**
//	 * Initialization fragment.
//	 */
//	protected Fragment initialization;

//	/**
//	 * Positions of connection init blocks.
//	 */
//	protected List< InitConnectionFragment > connectionInits = new ArrayList< InitConnectionFragment >();

	/**
	 * The name of the version control table as defined in the upgrade file.
	 */
	protected String versionTableName;

	/**
	 * The name of the log control table as defined in the upgrade file.
	 */
	protected String logTableName;


	/**
	 * Constructor.
	 *
	 * @param file The reader which is used to read the contents of the file.
	 */
	protected UpgradeFile( RandomAccessLineReader file )
	{
		this.file = file;

		String line = file.readLine();
		//System.out.println( "First line [" + line + "]" );
		StringBuilder s = new StringBuilder();
		char[] chars = line.toCharArray();
		for( char c : chars )
			if( c != 0 )
				s.append( c );

		line = s.toString();
		//System.out.println( "First line (fixed) [" + line + "]" );
		Matcher matcher = SQLFile.ENCODING_PATTERN.matcher( line );
		if( matcher.matches() )
		{
			file.reOpen( matcher.group( 1 ) );
			file.readLine(); // skip the first line
		}
		else
			file.gotoLine( 1 );
	}


	/**
	 * Translates a segment type string to a type enum.
	 *
	 * @param type A segment type string.
	 * @return The corresponding type enum.
	 */
	protected Type stringToType( String type )
	{
		if( "UPGRADE".equalsIgnoreCase( type ) || "PATCH".equalsIgnoreCase( type ) )
			return Type.UPGRADE;
		if( "SWITCH".equalsIgnoreCase( type ) || "BRANCH".equalsIgnoreCase( type ) || "RETURN".equalsIgnoreCase( type ) )
			return Type.SWITCH;
		if( "DOWNGRADE".equalsIgnoreCase( type ) )
			return Type.DOWNGRADE;
		if( "SETUP".equalsIgnoreCase( type ) || "INIT".equalsIgnoreCase( type ) )
			return Type.SETUP;
		Assert.fail( "Unexpected block type '" + type + "'" );
		return null;
	}


	/**
	 * Scans for segments in the file.
	 */
	protected void scan()
	{
		boolean withinDefinition = false;
		boolean definitionComplete = false;
		while( !definitionComplete )
		{
			String line = this.file.readLine();
			if( line == null )
				throw new CommandFileException( "Unexpected EOF found", this.file.getLineNumber() );

			if( line.trim().length() > 0 )
			{
				Assert.isTrue( line.startsWith( "--*" ), "Line should start with --*" );
				line = line.substring( 3 ).trim();
				if( line.equalsIgnoreCase( "DEFINITION" ) || line.equalsIgnoreCase( "PATCHES" ) )
				{
					Assert.isFalse( withinDefinition, "Already within the definition" );
					withinDefinition = true;
				}
				else if( line.startsWith( "//" ) )
				{
					// ignore line
				}
				else if( PATCH_DEFINITION_MARKER_PATTERN.matcher( line ).matches() )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );

					Matcher matcher = PATCH_DEFINITION_PATTERN.matcher( line );
					Assert.isTrue( matcher.matches(), PATCH_DEFINITION_SYNTAX_ERROR );
					String action = matcher.group( 1 );
					boolean open = matcher.group( 2 ) != null;
					String source = matcher.group( 3 );
					if( source.length() == 0 )
						source = null;
					String target = matcher.group( 4 );
					Type type = stringToType( action );
					UpgradeSegment segment = new UpgradeSegment( type, source, target, open );
					if( type == Type.SETUP )
					{
						if( this.setups.containsKey( source ) )
							throw new CommandFileException( "Duplicate definition of init block for source version " + source, this.file.getLineNumber() - 1 );
						this.setups.put( source, segment );
					}
					else
					{
						Collection< UpgradeSegment > segments = this.segments.get( source );
						if( segments == null )
							this.segments.put( source, segments = new LinkedList< UpgradeSegment >() );
						segments.add( segment );
						this.versions.add( source );
						this.versions.add( target );
					}
				}
				else if( line.equalsIgnoreCase( "/DEFINITION" ) || line.equalsIgnoreCase( "/PATCHES" ) )
				{
					Assert.isTrue( withinDefinition, "Not within the definition" );
					definitionComplete = true;
				}
				else if( withinDefinition )
				{
					Matcher matcher;
					if( ( matcher = CONTROL_TABLES_PATTERN.matcher( line ) ).matches() )
					{
						this.versionTableName = matcher.group( 1 );
						this.logTableName = matcher.group( 2 );
					}
					else if( ( matcher = CommandProcessor.delimiterPattern.matcher( line ) ).matches() )
						this.defaultDelimiters = CommandProcessor.parseDelimiters( matcher );
					else
						throw new SystemException( "Unexpected line within definition: " + line );
				}
				else
					throw new SystemException( "Unexpected line outside definition: " + line );
			}
		}

		String line = this.file.readLine();
		while( line != null )
		{
			if( line.startsWith( "--*" ) )
			{
				Matcher matcher;
				/*
				if( ( matcher = INITIALIZATION_TRIGGER.matcher( line ) ).matches() )
				{
					line = this.file.readLine();
					if( line == null )
						throw new CommandFileException( "Premature EOF found", this.file.getLineNumber() );
					int mode = 1;
					int pos = -1;
					StringBuilder builder = new StringBuilder();
					while( line != null && !INITIALIZATION_END_PATTERN.matcher( line ).matches() )
					{
						if( mode == 1 )
							if( !StringUtils.isBlank( line ) )
							{
								mode = 2;
								pos = this.file.getLineNumber() - 1;
							}

						if( mode == 2 )
						{
							if( this.file.getLineNumber() > pos + 1000 )
								throw new CommandFileException( "INITIALIZATION block exceeded maximum line count of 1000", pos );
							builder.append( line );
							builder.append( '\n' );
						}

						line = this.file.readLine();
					}

					if( mode == 2 )
						this.initialization = new Fragment( pos, builder.toString() );
				}
				/* else if( ( matcher = INIT_CONNECTION_TRIGGER.matcher( line ) ).matches() )
				{
					int mode = 1;
					int pos = -1;
					StringBuilder builder = new StringBuilder();
					ArrayList< InitConnectionFragment > inits = new ArrayList< InitConnectionFragment >();
					while( line != null && !INIT_CONNECTION_END_PATTERN.matcher( line ).matches() )
					{
						if( INIT_CONNECTION_TRIGGER.matcher( line ).matches() ) // Detect all markers
						{
							if( mode != 1 )
								throw new CommandFileException( "INIT CONNECTION blocks can only be strictly nested", this.file.getLineNumber() - 1 );
							if( !( matcher = INIT_CONNECTION_PARSER.matcher( line ) ).matches() )
								throw new CommandFileException( INIT_CONNECTION_SYNTAX, this.file.getLineNumber() - 1 );
							inits.add( new InitConnectionFragment( matcher.group( 1 ), matcher.group( 2 ) ) );
						}
						else
						{
							if( mode == 1 )
								if( !StringUtils.isBlank( line ) )
								{
									mode = 2;
									pos = this.file.getLineNumber() - 1;
								}

							if( mode == 2 )
							{
								if( this.file.getLineNumber() > pos + 1000 )
									throw new CommandFileException( "INIT CONNECTION block exceeded maximum line count of 1000", pos );
								builder.append( line );
								builder.append( '\n' );
							}
						}

						line = this.file.readLine();
					}

					if( mode == 2 )
						for( InitConnectionFragment initConnectionFragment : inits )
						{
							initConnectionFragment.setText( pos, builder.toString() );
							this.connectionInits.add( initConnectionFragment );
						}
				}
				else
				 */
				if( PATCH_START_MARKER_PATTERN.matcher( line ).matches() )
				{
					int pos = this.file.getLineNumber() - 1;
					line = line.substring( 3 ).trim();
					matcher = PATCH_START_PATTERN.matcher( line );
					if( !matcher.matches() )
						throw new CommandFileException( MARKER_SYNTAX_ERROR, pos );
					String action = matcher.group( 1 );
					String source = matcher.group( 2 );
					String target = matcher.group( 3 );
					Type type = stringToType( action );
					UpgradeSegment segment;
					if( type == Type.SETUP )
					{
						segment = getSetupSegment( source.length() == 0 ? null : source, target );
						if( segment == null )
							throw new CommandFileException( "Undefined setup block found: \"" + source + "\" --> \"" + target + "\"", pos );
					}
					else
					{
						segment = getSegment( source.length() == 0 ? null : source, target );
						if( segment == null )
							throw new CommandFileException( "Undefined upgrade block found: \"" + source + "\" --> \"" + target + "\"", pos );
						if( segment.getType() != type )
							throw new CommandFileException( "Upgrade block type '" + action + "' is different from its definition", pos );
					}
					if( segment.getLineNumber() >= 0 )
						throw new CommandFileException( "Duplicate upgrade block \"" + source + "\" --> \"" + target + "\" found", pos );
					segment.setLineNumber( pos );
				}
			}

			line = this.file.readLine();
		}

		// Check that all defined upgrade blocks are found
		for( Collection< UpgradeSegment > segments : this.segments.values() )
			for( UpgradeSegment segment : segments )
				if( segment.getLineNumber() < 0 )
					throw new FatalException( "Upgrade block \"" + StringUtils.defaultString( segment.getSource() ) + "\" --> \"" + segment.getTarget() + "\" not found" );

		// Check that all defined setup blocks are found
		for( UpgradeSegment segment : this.setups.values() )
			if( segment.getLineNumber() < 0 )
				throw new FatalException( "Setup block \"" + StringUtils.defaultString( segment.getSource() ) + "\" --> \"" + segment.getTarget() + "\" not found" );
	}


	/**
	 * Gets the encoding of the upgrade file.
	 *
	 * @return The encoding of the upgrade file.
	 */
	public String getEncoding()
	{
		return this.file.getEncoding();
	}


	/**
	 * Close the file and all underlying streams/readers.
	 */
	protected void close()
	{
		if( this.file != null )
		{
			this.file.close();
			this.file = null;
		}
	}


	/**
	 * Returns the upgrade segment belonging to the specified source and target. Also checks for duplicates.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @return The corresponding upgrade segment.
	 */
	protected UpgradeSegment getSegment( String source, String target )
	{
		UpgradeSegment result = null;

		Collection< UpgradeSegment > segments = this.segments.get( source );
		if( segments != null )
			for( UpgradeSegment segment : segments )
				if( segment.getTarget().equals( target ) )
				{
					if( result != null )
						throw new CommandFileException( "Duplicate upgrade block found", segment.getLineNumber() );
					result = segment;
				}

		return result;
	}


	/**
	 * Returns the setup segment belonging to the specified source and target. Also checks for duplicates.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @return The corresponding segment.
	 */
	protected UpgradeSegment getSetupSegment( String source, String target )
	{
		UpgradeSegment segment = this.setups.get( source );
		if( segment.getTarget().equals( target ) )
			return segment;
		return null;
	}


	/**
	 * Determine the best path between a source version and a target version.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @param downgradesAllowed Allow downgrades in the resulting path.
	 * @return The best path between a source version and a target version. This path can be empty when the source and
	 *         target are equal. The result will be null if there is no path.
	 */
	protected Path getUpgradePath( String source, String target, boolean downgradesAllowed )
	{
		Set< String > done = new HashSet< String >();
		done.add( source );
		return getUpgradePath0( source, target, downgradesAllowed, done );
	}


	/**
	 * Determine the best path between a source version and a target version.
	 *
	 * @param source The source version.
	 * @param target The target version.
	 * @param downgradesAllowed Allow downgrades in the resulting path.
	 * @param targetsProcessed A set of targets that have already been processed. This prevents endless loops.
	 * @return The best path between a source version and a target version. This path can be empty when the source and
	 *         target are equal. The result will be null if there is no path.
	 */
	protected Path getUpgradePath0( String source, String target, boolean downgradesAllowed, Set< String > targetsProcessed )
	{
		Path result = new Path();

		// If equal than return an empty path
		if( ObjectUtils.equals( source, target ) )
			return result;

		// Start with all the segments that have the given source
		Collection< UpgradeSegment > segments = this.segments.get( source );

		// As long as only one segment found loop instead of recursion
		while( segments != null && segments.size() == 1 )
		{
			UpgradeSegment segment = segments.iterator().next();

			if( targetsProcessed.contains( segment.getTarget() ) ) // Target already processed -> no path found
				return null;

			targetsProcessed.add( segment.getTarget() ); // Register target

			result.append( segment ); // Append to result
			if( target.equals( segment.getTarget() ) ) // Target is requested target -> return result
				return result;

			segments = this.segments.get( segment.getTarget() );
		}

		// No segments -> no path found
		if( segments == null )
			return null;

		// More then one segment found, select the best one
		Path selected = null;
		for( UpgradeSegment segment : segments )
		{
			if( targetsProcessed.contains( segment.getTarget() ) ) // Target already processed -> ignore
				continue;

			// Build new set for recursive call
			Set< String > processed = new HashSet< String >();
			processed.addAll( targetsProcessed );
			processed.add( segment.getTarget() );

			// Call recursive and select if better
			Path path = getUpgradePath0( segment.getTarget(), target, downgradesAllowed, processed );
			if( path != null )
			{
				path.prepend( segment );
				if( selected == null )
					selected = path;
				else if( path.betterThan( selected ) )
					selected = path;
			}
		}

		// No segments found -> no path found
		if( selected == null )
			return null;

		// Return the selected path appended to the first path
		return result.append( selected );
	}


	/**
	 * Returns a setup path for the specified source. As setup is always done to the latest version, no target is needed.
	 *
	 * @param source The source version.
	 * @return A list of setup segments that correspond to the given source.
	 */
	protected List< UpgradeSegment > getSetupPath( String source )
	{
		List< UpgradeSegment > result = new ArrayList< UpgradeSegment >();

		// Branches not possible

		// Start with all the segments that start with the given source
		UpgradeSegment segment = this.setups.get( source );
		while( segment != null )
		{
			result.add( segment );
			segment = this.setups.get( segment.getTarget() );
		}

		if( result.size() > 0 )
			return result;
		return null;
	}


	/**
	 * Determines all possible target versions from the specified source version. The current version is also considered.
	 *
	 * @param version Current version.
	 * @param targeting Indicates that we are already targeting a specific version.
	 * @param tips Only return tip versions.
	 * @param downgradesAllowed Allow downgrades.
	 * @param prefix Only consider versions that start with the given prefix.
	 * @param result All results are added to this set.
	 */
	protected void collectTargets( String version, String targeting, boolean tips, boolean downgradesAllowed, String prefix, Set< String > result )
	{
		Assert.notNull( result, "'result' must not be null" );

		// Will throw errors when the version or targeting version is not available in the upgrade file
		collectReachableVersions( version, targeting, downgradesAllowed, result );

		// Filter out all version that do not start with the prefix
		if( prefix != null )
			for( Iterator< String > iterator = result.iterator(); iterator.hasNext(); )
			{
				String s = iterator.next();
				if( s == null || !s.startsWith( prefix ) )
					iterator.remove();
			}

		// Filter out all versions that are not the tips of the reachable path
		if( tips )
			for( Iterator< String > iterator = result.iterator(); iterator.hasNext(); )
			{
				String v = iterator.next();
				Collection< UpgradeSegment > segments = this.segments.get( v );
				if( segments != null )
					for( UpgradeSegment segment : segments )
						if( segment.isUpgrade() )
							if( prefix == null || segment.getTarget().startsWith( prefix ) )
							{
								iterator.remove();
								break;
							}
			}
	}


	/**
	 * Gets all versions that are reachable from the given source version.
	 *
	 * @param source The source version.
	 * @param targeting Indicates that we are already targeting a specific version.
	 * @param downgradesAllowed Allow downgrades.
	 * @return Returns an ordered set of versions that are reachable from the given source version.
	 */
	protected LinkedHashSet< String > getReachableVersions( String source, String targeting, boolean downgradesAllowed )
	{
		LinkedHashSet< String > result = new LinkedHashSet< String >();
		collectReachableVersions( source, targeting, downgradesAllowed, result );
		return result;
	}


	/**
	 * Retrieves all versions that are reachable from the given source version. The current version is also considered.
	 *
	 * @param source The source version.
	 * @param targeting Already targeting a specific version.
	 * @param downgradesAllowed Allow downgrades.
	 * @param result This set gets filled with all versions that are reachable from the given source version.
	 */
	protected void collectReachableVersions( String source, String targeting, boolean downgradesAllowed, Set< String > result )
	{
		if( !this.versions.contains( source ) )
			throw new FatalException( "The current database version " + StringUtils.defaultString( source, "<no version>" ) + " is not available in the upgrade file. Maybe this version is deprecated or the wrong upgrade file is used." );

		if( targeting == null )
			result.add( source ); // The source is reachable

		Collection< UpgradeSegment > segments = this.segments.get( source ); // Get all segments with the given source
		if( segments == null )
			return;

		// Queue contains segments that await processing
		LinkedList< UpgradeSegment > queue = new LinkedList< UpgradeSegment >();

		// Fill queue with segments
		if( targeting != null )
		{
			for( UpgradeSegment segment : segments )
				if( targeting.equals( segment.getTarget() ) )
					queue.add( segment ); // Add segment to the end of the list
			if( queue.isEmpty() )
				throw new FatalException( "The database is incompletely upgraded to version " + targeting + ", but that version is not reachable from version " + StringUtils.defaultString( source, "<no version>" ) );
		}
		else
			queue.addAll( segments );

		// Process the queue
		while( !queue.isEmpty() )
		{
			UpgradeSegment segment = queue.removeFirst(); // pop() is not available in java 5
			if( !result.contains( segment.getTarget() ) ) // Already there?
				if( downgradesAllowed || !segment.isDowngrade() ) // Downgrades allowed?
				{
					result.add( segment.getTarget() );
					if( !segment.isOpen() ) // Stop when segment is open.
					{
						segments = this.segments.get( segment.getTarget() ); // Add the next to the queue
						if( segments != null )
							queue.addAll( segments ); // Add segments to the end of the list
					}
				}
		}
	}


	/**
	 * Jump to the position in the upgrade file where the given segment starts.
	 *
	 * @param segment The upgrade segment to jump to.
	 * @return The source for the given segment.
	 */
	protected UpgradeSource gotoSegment( UpgradeSegment segment )
	{
		Assert.isTrue( segment.getLineNumber() >= 0, "Upgrade or setup block not found" );

		this.file.gotoLine( segment.getLineNumber() );
		String line = this.file.readLine();
//		System.out.println( line );
		Assert.isTrue( PATCH_START_MARKER_PATTERN.matcher( line ).matches() );
		UpgradeSource source = new UpgradeSource( this.file );
		source.setDelimiters( this.defaultDelimiters );
		return source;
	}
}
