package ch.noisette.io.httpstream.channel;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import org.jboss.netty.channel.FileRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FileRegion} that is able to repeat a given amount of time (or indefinitely} a file.
 * 
 * @author bperroud
 *
 */
public class RepeatingFileRegion
    implements FileRegion
{
    protected final Logger log = LoggerFactory.getLogger( RepeatingFileRegion.class );

    private final FileChannel file;

    private final long fileLength;

    private final Integer numberOfTimesToRepeat;

    private final long totalSize;

    public RepeatingFileRegion( FileChannel file, long fileLength, Integer numberOfTimesToRepeat )
    {
        this.file = file;
        this.fileLength = fileLength;
        this.numberOfTimesToRepeat = numberOfTimesToRepeat;
        if ( this.numberOfTimesToRepeat == null )
        {
            totalSize = Long.MAX_VALUE;
        }
        else
        {
            totalSize = this.fileLength * this.numberOfTimesToRepeat;
        }
    }

    public long getPosition()
    {
        return 0;
    }

    public long getCount()
    {
        return totalSize;
    }

    public boolean releaseAfterTransfer()
    {
        return true;
    }

    public long transferTo( WritableByteChannel target, long position )
        throws IOException
    {

        long fileOffset = position % fileLength;
        return file.transferTo( fileOffset, fileLength - fileOffset, target );

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
