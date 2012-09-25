package ch.noisette.io.httpstream;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

import org.junit.Test;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;

public class BasicStreamTest
    extends AbstractRepeatingStreamTest
{

    @Test
    public void asyncStreamGETRepeat10Test()
        throws Throwable
    {
        final CountDownLatch l = new CountDownLatch( 1 );
        final AsyncHttpClient c = new AsyncHttpClient();

        final String url = "http://127.0.0.1:" + server.getPort() + "/test1.txt?10";
//        final String url = "http://127.0.0.1:" + server.getPort() + "/../../../../../../etc/passwd?10";

        final ListenableFuture<String> f = c.prepareGet( url ).execute( new AsyncHandler<String>()
        {

            private final AtomicLong c = new AtomicLong();

            public void onThrowable( Throwable t )
            {
                t.printStackTrace();
                try
                {
                    Assert.fail( "Unexpected exception: " + t.getMessage() );
                }
                finally
                {
                    l.countDown();
                }
            }

            /* @Override */
            public STATE onBodyPartReceived( final HttpResponseBodyPart content )
                throws Exception
            {

                final ByteBuffer buffer = content.getBodyByteBuffer();
                c.addAndGet( buffer.capacity() );

                return STATE.CONTINUE;
            }

            /* @Override */
            public STATE onStatusReceived( final HttpResponseStatus responseStatus )
                throws Exception
            {

                Assert.assertEquals( org.jboss.netty.handler.codec.http.HttpResponseStatus.OK.getCode(),
                                     responseStatus.getStatusCode() );

                return STATE.CONTINUE;
            }

            /* @Override */
            public STATE onHeadersReceived( final HttpResponseHeaders headers )
                throws Exception
            {

                FluentCaseInsensitiveStringsMap h = headers.getHeaders();
                Assert.assertNotNull( h );

                return STATE.CONTINUE;
            }

            /* @Override */
            public String onCompleted()
                throws Exception
            {

                l.countDown();
                LOG.info( "Read " + c.get() + " bytes" );

                Assert.assertEquals( 1100, c.get() );

                return "OK";
            }

        } );

        if ( !l.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "Timeout out" );
        }
        c.close();

        final String completedString = f.get();
        Assert.assertEquals( "OK", completedString );

    }
    
    @Test
    public void asyncStreamGETForbiddenFileTest()
        throws Throwable
    {
        final CountDownLatch l = new CountDownLatch( 1 );
        final AsyncHttpClient c = new AsyncHttpClient();

        final String url = "http://127.0.0.1:" + server.getPort() + "/../../../../../../etc/passwd?1";

        final ListenableFuture<String> f = c.prepareGet( url ).execute( new AsyncHandler<String>()
        {

            private final AtomicLong c = new AtomicLong();

            public void onThrowable( Throwable t )
            {
                t.printStackTrace();
                try
                {
                    Assert.fail( "Unexpected exception: " + t.getMessage() );
                }
                finally
                {
                    l.countDown();
                }
            }

            /* @Override */
            public STATE onBodyPartReceived( final HttpResponseBodyPart content )
                throws Exception
            {

                final ByteBuffer buffer = content.getBodyByteBuffer();
                c.addAndGet( buffer.capacity() );

                return STATE.CONTINUE;
            }

            /* @Override */
            public STATE onStatusReceived( final HttpResponseStatus responseStatus )
                throws Exception
            {

                Assert.assertEquals( org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN.getCode(),
                                     responseStatus.getStatusCode() );

                return STATE.ABORT;
            }

            /* @Override */
            public STATE onHeadersReceived( final HttpResponseHeaders headers )
                throws Exception
            {
                return STATE.CONTINUE;
            }

            /* @Override */
            public String onCompleted()
                throws Exception
            {

                l.countDown();
                LOG.info( "Read " + c.get() + " bytes" );

                Assert.assertEquals( 0, c.get() );

                return "OK";
            }

        } );

        if ( !l.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "Timeout out" );
        }
        c.close();

        final String completedString = f.get();
        Assert.assertEquals( "OK", completedString );

    }
    

}
