package ch.noisette.io.httpstream;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpMethod.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

// import javax.activation.MimetypesFileTypeMap;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.noisette.io.httpstream.channel.RepeatingFileRegion;

/**
 * 
 * Read file with name equals to request URI from rootDir. Send the file to the caller with Zero-Copy.
 * 
 * @author bperroud
 * 
 * Heavily inspired from {@see io.netty.example.http.file.HttpStaticFileServerHandler}.
 * 
 */
public class HttpStreamServerHandler
    extends SimpleChannelUpstreamHandler
{
    protected final Logger log = LoggerFactory.getLogger( HttpStreamServerHandler.class );

    private final String rootDir;

    public HttpStreamServerHandler( final String rootDir )
    {
        this.rootDir = rootDir;
    }

    @Override
    public void messageReceived( ChannelHandlerContext ctx, MessageEvent e )
        throws Exception
    {
        HttpRequest request = (HttpRequest) e.getMessage();
        if ( request.getMethod() != GET )
        {
            sendError( ctx, METHOD_NOT_ALLOWED );
            return;
        }

        final File file = sanitizeUri( request.getUri() );
        if ( file == null )
        {
            sendError( ctx, FORBIDDEN );
            return;
        }

        if ( file.isHidden() || !file.exists() )
        {
            sendError( ctx, NOT_FOUND );
            return;
        }
        if ( !file.isFile() )
        {
            sendError( ctx, FORBIDDEN );
            return;
        }

        Integer numberOfTimesToRepeat = null;
        int pos = request.getUri().indexOf( '?' );
        if ( pos > 0 )
        {
            try
            {
                numberOfTimesToRepeat = Integer.parseInt( request.getUri().substring( pos + 1 ) );
            }
            catch ( NumberFormatException nfe )
            {
                numberOfTimesToRepeat = 1;
            }
        }

        RandomAccessFile raf;
        try
        {
            raf = new RandomAccessFile( file, "r" );
        }
        catch ( FileNotFoundException fnfe )
        {
            sendError( ctx, NOT_FOUND );
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse( HTTP_1_1, OK );

        Channel ch = e.getChannel();

        // Write the initial line and the header.
        ch.write( response );

        // Write the content.
        final FileRegion region = new RepeatingFileRegion( raf.getChannel(), fileLength, numberOfTimesToRepeat );

        final ChannelFuture writeFuture = ch.write( region );
        writeFuture.addListener( new ChannelFutureProgressListener()
        {
            //                @Override
            public void operationComplete( ChannelFuture future )
            {
                region.releaseExternalResources();
            }

            //                @Override
            public void operationProgressed( ChannelFuture future, long amount, long current, long total )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( String.format( "%s: %d / %d (+%d)%n", file.getAbsolutePath(), current, total, amount ) );
                }
            }
        } );

        // Force closing the connection.
        writeFuture.addListener( ChannelFutureListener.CLOSE );
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e )
        throws Exception
    {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if ( cause instanceof TooLongFrameException )
        {
            sendError( ctx, BAD_REQUEST );
            return;
        }

        cause.printStackTrace();
        if ( ch.isConnected() )
        {
            sendError( ctx, INTERNAL_SERVER_ERROR );
        }
    }

    private File sanitizeUri( String uri ) throws IOException
    {
        // Decode the path.
        try
        {
            uri = URLDecoder.decode( uri, "UTF-8" );
        }
        catch ( UnsupportedEncodingException e )
        {
            try
            {
                uri = URLDecoder.decode( uri, "ISO-8859-1" );
            }
            catch ( UnsupportedEncodingException e1 )
            {
                throw new IllegalArgumentException( "Uri " + uri + " can't be decoded." );
            }
        }

        // Convert file separators.
        uri = uri.replace( '/', File.separatorChar );

        // remove query string part
        int pos = uri.indexOf( '?' );
        if ( pos > 0 )
        {
            uri = uri.substring( 0, pos );
        }

        // Convert to absolute path.
        final File file = new File( rootDir + File.separator + uri );

        // Ensure the requested file is still inside the root directory
        if ( file.getCanonicalPath().startsWith( rootDir ) )
        {
            return file;
        }
        else
        {
            return null;
        }
    }

    private void sendError( ChannelHandlerContext ctx, HttpResponseStatus status )
    {
        HttpResponse response = new DefaultHttpResponse( HTTP_1_1, status );
        response.setHeader( CONTENT_TYPE, "text/plain; charset=UTF-8" );
        response
            .setContent( ChannelBuffers.copiedBuffer( "Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8 ) );

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write( response ).addListener( ChannelFutureListener.CLOSE );
    }

}
