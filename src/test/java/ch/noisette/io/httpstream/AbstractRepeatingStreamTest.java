package ch.noisette.io.httpstream;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.noisette.io.httpstream.HttpStreamServer;

public abstract class AbstractRepeatingStreamTest
{

    protected final Logger LOG = LoggerFactory.getLogger( getClass() );

    protected HttpStreamServer server;

    @Before
    public void before()
        throws IOException
    {

        server = HttpStreamServerMain.runInSeparatedThread();

    }


    @After
    public void after()
        throws IOException
    {

        server.close();

    }

}
