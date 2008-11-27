package ronnie.dbpatcher.test.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.output.TeeOutputStream;

import ronnie.dbpatcher.Console;

import com.logicacmg.idt.commons.util.Assert;

public class MockConsole extends Console
{
	protected Queue< String > answerQueue = new LinkedList< String >();
	protected ByteArrayOutputStream buffer;

	protected MockConsole()
	{
		this.prefixWithDate = false;
		this.buffer = new ByteArrayOutputStream();
		this.out = new PrintStream( new TeeOutputStream( this.buffer, System.out ) );
		//this.out = new PrintStream( this.buffer );
	}

	protected void addAnswer( String answer )
	{
		this.answerQueue.offer( answer );
	}

	protected String getOutput()
	{
		return this.buffer.toString();
	}

	@Override
	protected synchronized String input() throws IOException
	{
		String input = this.answerQueue.poll();
		Assert.notNull( input, "No more input" );

		this.col = 0;
		return input;
	}
}
