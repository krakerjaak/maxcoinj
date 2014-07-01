# MaxcoinJ - The Java wallet library

This is a port of the BitcoinJ project to make the library work with the Maxcoin protocol.

The code has been built to be as non-invasive as possible with regards to the original BitcoinJ code.
By limiting the changes to the original code base, we make it easier to port back any future improvement 
contributed by the parent BitcoinJ project's contributors.

## Where to start?

Have a look at the 'maxcoin' directory, where a specific Maven module sits and contains all
Maxcoin-specific customizations.

There is also a sample app that can be started using class `com.google.bitcoin.examples.ForwardingService` (in the `maxcoin` module).
This class has been blatantly copied off the original project but slightly modified to enable Maxcoin modifications.

## Project status

At the time of this writing, the library is able to download the complete Maxcoin blockchain, generate Maxcoin wallet addresses, and see transactions.
The ForwardingService example is able to see incoming transactions, but I have yet to fully test the forwarding functionality in the example.

## Original `readme` below:

To get started, ensure you have the latest JDK installed, and download Maven from:

[http://maven.apache.org/]

Then run "mvn clean package" to compile the software. You can also run "mvn site:site" to generate a website with
useful information like JavaDocs. The outputs are under the target/ directory.

Alternatively, just import the project using your IDE. IntelliJ has Maven integration once you tell it where to
find your unzipped Maven install directory.

Now try running one of the example apps:

    cd examples
    mvn exec:java -Dexec.mainClass=com.google.bitcoin.examples.ForwardingService -Dexec.args="<insert a bitcoin address here>"

It will download the block chain and eventually print a Bitcoin address. If you send coins to it,
it will forward them on to the address you specified. Note that this example app does not use
checkpointing, so the initial chain sync will be pretty slow. You can make an app that starts up and
does the initial sync much faster by including a checkpoints file; see the documentation for
more info on this.

Now you are ready to follow the tutorial:

[https://bitcoinj.github.io/getting-started]

