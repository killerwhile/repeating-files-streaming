package ch.noisette.io.httpstream;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpStreamServer
    implements Runnable, Closeable
{
    protected final Logger log = LoggerFactory.getLogger( HttpStreamServer.class );

    private final int port;

    private final String rootDir;

    public HttpStreamServer( int port, String rootDir )
    {
        this.port = port;
        this.rootDir = rootDir;
    }

    private ServerBootstrap bootstrap = null;

    private Channel serverChannel = null;

    public void run()
    {

        log.info( "Starting " + HttpStreamServer.class.getSimpleName() + " ..." );

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                                                         new NioServerSocketChannelFactory( Executors
                                                             .newCachedThreadPool(), Executors.newCachedThreadPool() ) );

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory( new HttpStreamServerPipelineFactory( rootDir ) );

        // Bind and start to accept incoming connections.
        serverChannel = bootstrap.bind( new InetSocketAddress( port ) );

        log.info( HttpStreamServer.class.getSimpleName() + " successfully started on port " + port
            + ", ready to serve files from " + rootDir + "." );
    }

    public void close()
        throws IOException
    {

        log.info( "Shutting down " + HttpStreamServer.class.getSimpleName() + " ..." );

        try
        {
            if ( serverChannel != null )
            {
                final ChannelFuture f = serverChannel.close();
                f.awaitUninterruptibly();
            }
        }
        finally
        {
            if ( bootstrap != null )
            {
                bootstrap.releaseExternalResources();
            }
        }

        log.info( HttpStreamServer.class.getSimpleName() + " successfully shut down." );
    }

    public int getPort()
    {
        return port;
    }

}
