package ch.noisette.io.httpstream;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpStreamServerMain
{

    public static void main( String[] args )
        throws IOException
    {
        int port;
        if ( args.length > 0 )
        {
            port = Integer.parseInt( args[0] );
        }
        else
        {
            port = findFreePort();
        }

        String rootDir;
        if ( args.length > 1 )
        {
            rootDir = args[1];
        }
        else
        {
            rootDir = getDefaultRootDir();
        }

        final HttpStreamServer server = runInSeparatedThread( port, rootDir );

        Runtime.getRuntime().addShutdownHook( new Thread()
        {

            @Override
            public void run()
            {
                try
                {
                    server.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                    // Can't do much more than that...
                }
            }

        } );
    }

    private static String getDefaultRootDir()
    {
        return System.getProperty( "user.dir" ) + File.separator + "repeating-files";
    }

    public static HttpStreamServer runInSeparatedThread()
        throws IOException
    {
        return runInSeparatedThread( findFreePort(), getDefaultRootDir() );
    }

    public static HttpStreamServer runInSeparatedThread( int port, String rootDir )
        throws IOException
    {

        ExecutorService executor = Executors.newFixedThreadPool( 1 );

        HttpStreamServer server = new HttpStreamServer( port, rootDir );

        executor.execute( server );

        executor.shutdown();

        return server;
    }

    protected static int findFreePort()
        throws IOException
    {
        ServerSocket socket = null;

        try
        {
            socket = new ServerSocket( 0 );

            return socket.getLocalPort();
        }
        finally
        {
            if ( socket != null )
            {
                socket.close();
            }
        }
    }

}
