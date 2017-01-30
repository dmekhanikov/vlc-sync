# VLC Playback Synchronization

Plugin for VideoLAN and a server for playback synchronization of multiple
instances of the player running on different machines.

It is made for people who want to watch movies together while being at
different places.

## Disclaimer
This plugin is not aimed to make multiple devices play videos precisely
synchronously. Half-second latency is not considered to be a problem.
If you need absolute synchronization, better use something else or
consider making a pull request ;-)

## Description
This project consists of two parts:

* server - Java app
* client - VideoLAN plugin

Server receives connections to TCP socket on port 7773 and replicates
actions performed by connected clients.

### Supported actions

* Play
* Pause
* Jump to position

## Running the server
To build the server you are going to need the following:

* JDK 1.8
* Apache Maven

Build command:

    $ mvn package

Executable jar file will appear at the `target` directory.

To start the server run:

    $ java -jar <path to the jar file>

## Running VLC player
Required version of VLC is `2.2.1+`

Copy `src/lua/sync.lua` to the `<VLC dir>/lua/intf/` directory. This
directory should already exist.

After that you can start your VLC player by running:

    $ vlc --extraintf luaintf --lua-intf sync --lua-config "sync={host='<host>'}"

Replace `<host>` with IP address of the server. `localhost` is used
by default. Make sure that server is up and running.
