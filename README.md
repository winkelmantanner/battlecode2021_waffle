# battlecode2021

My code for MIT BattleCode 2021.

Tanner's notes on how to make this work:

Tanner's Mac has 2 versions of Java in /Library/Java/JavaVirtualMachines/

The current one is determined by the env variable JAVA_HOME

As of me writing this the default one is jdk-10.0.2.jdk

This battlecode stuff ONLY works if the other one is used.

So I have to run:

`export JAVA_HOME='/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home'`

Then run:

`./gradlew run`

from the same terminal.

`./gradlew run` opens the game in the client app automatically.

`./gradlew -q tasks` list gradlew commands

I tried setting JAVA_HOME in .MacOSX/environment.plist and it didn't fix it.
I had to create .MacOSX and environment.plist, so I deleted them after I saw that it didn't work.
I've also tried setting JAVA_HOME in ~/.bash_profile

BUT THIS WORKED:
Run the export command above then run:

`open client/Battlecode\ Client.app/`


The bot must be named tannerplayer.