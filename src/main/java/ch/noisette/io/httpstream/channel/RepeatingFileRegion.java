package ch.noisette.io.httpstream.channel;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import org.jboss.netty.channel.FileRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepeatingFileRegion
    implements FileRegion
{
    protected final Logger log = LoggerFactory.getLogger( RepeatingFileRegion.class );

    private final FileChannel file;

    private final long fileLength;

    private final Integer numberOfTimesToRepeat;

    public RepeatingFileRegion( FileChannel file, long fileLength, Integer numberOfTimesToRepeat )
    {
        this.file = file;
        this.fileLength = fileLength;
        this.numberOfTimesToRepeat = numberOfTimesToRepeat;
    }

    public long getPosition()
    {
        return 0;
    }

    public long getCount()
    {
        if ( numberOfTimesToRepeat == null )
        {
            return Long.MAX_VALUE;
        }
        else
        {
            return fileLength * numberOfTimesToRepeat;
        }
    }

    public boolean releaseAfterTransfer()
    {
        return true;
    }

    public long transferTo( WritableByteChannel target, long position )
        throws IOException
    {

        long transfered = 0;
        int i = 0;
        while ( transfered < fileLength )
        {
            transfered += internalTransferTo( target, transfered, fileLength );

            // watchdog to not infinite loop if the file is too big.
            if ( i++ > 10 )
            {
                break;
            }
        }

        return transfered;
    }

    private long internalTransferTo( WritableByteChannel target, long from, long to )
        throws IOException
    {

        if ( to - from <= 0 )
        {
            throw new IllegalArgumentException( "position out of range. " + " (expected: " + from + " - " + to + ")" );
        }

        return file.transferTo( from, to, target );

    }

    public void releaseExternalResources()
    {
        try
        {
            file.close();
        }
        catch ( IOException e )
        {
            if ( log.isWarnEnabled() )
            {
                log.warn( "Failed to close a file.", e );
            }
        }
    }

}
