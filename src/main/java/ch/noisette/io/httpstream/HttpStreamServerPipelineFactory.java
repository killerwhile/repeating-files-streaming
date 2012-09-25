package ch.noisette.io.httpstream;

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * 
 * @author bperroud
 * 
 * Heavily inspired from {@see io.netty.example.http.file.HttpStaticFileServerPipelineFactory}.
 * 
 */
public class HttpStreamServerPipelineFactory
    implements ChannelPipelineFactory
{

    private final String rootDir;

    public HttpStreamServerPipelineFactory( final String rootDir )
    {
        this.rootDir = rootDir;
    }

    public ChannelPipeline getPipeline()
        throws Exception
    {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast( "decoder", new HttpRequestDecoder() );
        pipeline.addLast( "aggregator", new HttpChunkAggregator( 65536 ) );
        pipeline.addLast( "encoder", new HttpResponseEncoder() );
        pipeline.addLast( "chunkedWriter", new ChunkedWriteHandler() );

        pipeline.addLast( "handler", new HttpStreamServerHandler( rootDir ) );

        return pipeline;
    }

}
