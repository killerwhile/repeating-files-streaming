Simple Files Streaming though HTTP
=========================

Java project that enables File Streaming through HTTP at blazing high throughput. Expect to max out your network interface. Based on [Netty.io](http://netty.io).

### Usage

#### Server Side

Build it using maven.

Then:

        java -cp target/repeating-files-streaming-1.0-SNAPSHOT.jar ch.noisette.io.httpstream.HttpStreamServerMain [portNumber [chrootDir]]

* `portNumber` set the port the streaming server will listen too. If not set, a free port will be randomly picked up
* `chrootDir` is the base directory from where file will be streamed. If not set, the default chroot dir is ${user.dir}/repeating-files. Particular care is taken to not go out of this chroot doitr

##### Client Side

        curl http://server:portNumber/relative/path/to/file[?numberOfTimesToRepeatTheFile]

* `numberOfTimesToRepeatTheFile` tells the number of times the file will be append to the stream before closing the connection. It not set, the file will be appened forever.

