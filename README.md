# battlecode2021_waffle

This code plays the MIT Battlecode 2021 game.  For more about this, see the [MIT Battlecode site](https://battlecode.org/) and the [2021 game resources](https://2021.battlecode.org/resources).  This year's game was politics themed.

This bot was known as "waffle" in the tournaments and on the ranklist.  This bot was a finalist, meaning it placed in the top 16 in the qualification tournament, marking the second year I was a finalist.  This will probably be my last year because only full-time students are eligible to win prizes in Battlecode and I expect to graduate soon.

Here are some facts about this bot.

### Enlightenment Centers (ECs)
- ECs try to build as many robots as they can.  If they don't have a lot of influence, they build muckrakers with just 1 influence.  This results in a map full of such muckrakers, which leads to games taking a very long time to run.
- If an EC sees an enemy muckraker, the EC sets `should_build_slans` to `false` permanently.  The EC will not build any slanderers for the rest of the game.  Converted ECs build slanderers just like initial ECs.
- ECs keep a large array of the IDs of all robots they have built.  Each round, they try to read the flag of every robot they have built.  But sometimes it takes more then one round because the EC gets near its bytecode limit.  The `Clock` class is utilized to check that there are enough bytecodes to safely continue reading flags.
- If an EC sees an enemy politician, the EC builds small muckrakers in the four cardinal directions.  If these muckrakers also see an enemy politician, they do not move.  These muckrakers serve as a shield for the EC because of the way empowering works.

### Politicians
- This bot divides its politicians into 2 kinds: *attackers* and *defenders*.
    - **Attackers** follow directions from the EC to find and attack neutral ECs and enemy ECs.  There are 3 ways attackers can be created:
        1. To convert neutral ECs.  Regarding these politicians:
            - The home EC builds at most 1 politician for each neutral EC.  It uses a HashMap to do this, using the MapLocation of the neutral EC to uniquely identify the neutral EC.  This is the only imported data structure used in the entire bot.  I tried to avoid using complex data structures because I was not sure how many bytecodes they would use.
            - The flag to report the neutral EC contains 4 bits indicating how much conviction the neutral EC has.  This allows the representation of 16 values which are given by the [CONV_VALS array](https://github.com/winkelmantanner/battlecode2021_waffle/blob/49d0a7c799c313e9bdf6f50a614cf4a91a3b4412/Robot.java#L190).  The EC sends a politician with just the right amount of influence to convert an EC with the conviction value indicated by the flag.
        2. If an EC has more than [2000 influence](https://github.com/winkelmantanner/battlecode2021_waffle/blob/49d0a7c799c313e9bdf6f50a614cf4a91a3b4412/EnlightenmentCenter.java#L345), it builds very large attacker politicians designed to convert enemy ECs.
        3. If a slanderer survives for 300 rounds, it is converted to a politician by the game.  These politicians are attacker politicians.
    - **Defenders** do not attack ECs but they attack other types of robots.  A flag from the home EC informes a newly-created politician that it is a defender.  Larger defenders stay near the home EC to protect slanderers, while smaller defenders explore the map.  I added code to use flagging to communicate the location and conviction of enemies among defenders.  While this code seems to work as expected, it had little effect on the performance of my bot.

### Muckrakers
- Small muckrakers (with just 1 influence) explore the map.  When any muckraker sees an enemy slanderer, it uses a flag to communicate with the EC that created it.  Then that EC broadcasts the same flag.  Then all muckrakers from that EC move toward the location of the slanderer.
- Large muckrakers (a.k.a. buffrakers) were one of the last features I added.  Here are some facts about this bot's large muckrakers:
    - [ECs build muckrakers of up to 500 influence in proportion to slanderers](https://github.com/winkelmantanner/battlecode2021_waffle/blob/49d0a7c799c313e9bdf6f50a614cf4a91a3b4412/EnlightenmentCenter.java#L309).  [ECs also build muckrakers of up to 50 influence if they recently received a flag about an enemy slanderer](https://github.com/winkelmantanner/battlecode2021_waffle/blob/49d0a7c799c313e9bdf6f50a614cf4a91a3b4412/EnlightenmentCenter.java#L378).
    - Large muckrakers are the only kind of robot that uses map symmetry.  Regarding map symmetry:
        - This year, map symmetry was much more difficult to use than it was last year.  That's because robots are not given the coordinates of the edges of the map in this year's game.  Instead, robots must discover the edges of the map and communicate the coordinates using flags.
        - Muckrakers and politicians use flags to communicate map edges to the home EC.  Then the home EC broadcasts the map edges regularly.
        - If a large muckraker knows 3 of the 4 map edges, it will move toward the unknown map edge.
        - Since the largest muckrakers are only built by ECs that have built slanderers, the symmetric enemy EC is likely to have built slanderers as well.  So it made since to me, the developer, to make large muckrakers go for the symmetric enemy EC.

### Slanderers
- Slanderers move randomly at first.  When any friendly robot sees any enemy robot, the friendly robot uses its flag to communicate the location and conviction of the enemy to the home EC.  The EC remembers the nearest enemy robot to itself and it broadcasts a flag indicating the position and conviction of this enemy robot.  Then all slanderers from that EC move toward the opposite side of the EC from the enemy that was flagged.
- When a slanderer is converted to a politican (after 300 rounds), the game does not re-instantiate the bot.  So I made the slanderers and politicians into one class: [SlanPol](https://github.com/winkelmantanner/battlecode2021_waffle/blob/49d0a7c799c313e9bdf6f50a614cf4a91a3b4412/SlanPol.java).
