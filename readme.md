## Java Pipelines ##
Years ago, I used to use a product on the IBM mainframe called [CMS Pipelines](http://www.vm.ibm.com/pipelines/).  It is an awesome 
record-based filtering system that lets you filter and change records as they flow through the pipeline.  Having left IBM some years ago, I no 
longer had access to a mainframe system where I could use this tool.

There is currently a NetREXX-based partial implementation of CMS Pipelines called _njPipes_, but given that I lack 1) a NetREXX installation, 
and 2) the inclination to find and install one, I thought that it would be an interesting exercise to try to write one in pure Java. 
 
I had a false start a few years ago when I realised that I didn't fully understand the whole record delay mechanism, but about 6 months 
ago, I had a situation arise where an implementation of Pipes would have been just the ticket.  Obviously that situation has long since passed, 
but it sowed the seed that I needed, and I re-downloaded all the [documentation](http://vm.marist.edu/~pipeline/) that I needed.

### Current features
While the implementation that I have is not complete, it definitely has the makings of a full featured implementation.  I have tried to
stick as closely as possible to the original implementation (notwithstanding the obvious Java/Rexx and EBCDIC/ASCII differences).  At times
this can be hard because I don't have a mainframe implementation to check my work with, but through careful reading of the manuals, I
believe that I have created a very faithful reproduction.

There are several features that are worth pointing out, I think:

* The dispatcher is complete with one exception: stall detection.  I have designed the stall detection mechanism, but have not yet implemented it.
* Single- and Multi-stream pipelines are fully supported.
* Callpipe has been implemented (but probably needs a little bit of testing).  Addpipe has been designed but not yet implemented.
* The dispatcher has the ability to generate events, which can be "listened to".  Eventually, this mechanism will be used to implement RUNPIPE EVENTS.
* Full support for MSGLEVEL, TRACE, STAGESEP, LISTCMD, LISTERR, et al (at both the pipe and stage level)
* While I have done very little performance tuning, I have been very conscious of throughput during the whole design and development process.

### Implemented Stages ###
The list below gives a taste for which stages have been implemented:

* <
* >
* >>
* ABBREV
* ADDRDW
* AGGRC
* BETWEEN
* BUFFER
* CHANGE
* CHOP
* COMMAND
* CONSOLE
* COUNT
* DUPLICATE
* FANIN
* FANINANY
* FANOUT
* GATE
* HOLE
* HOSTID
* HOSTNAME
* LITERAL
* LOCATE
* NLOCATE
* NOEOFBACK
* NOT
* QUERY
* REVERSE
* SPECS
* SPLIT
* STRLITERAL
* TAKE

### How to use? ###
Download `pipe.jar` from [github.com](https://github.com/edwardaux/Pipelines), and use a command like this one:

	java -jar pipe.jar "literal hello there | cons"

Alternatively, if you want to use it from within your existing java program, you could do something like:

	new Pipe().run("literal hello there | > blah");

### Bug Reports or Problems ###

Although I have many testcases for each of the stages, there is obviously some chance that bugs have slipped through. If you believe 
that you have found a bug, or a difference to the mainframe implementation, please [email](mailto:craig@hae.com.au) me.
