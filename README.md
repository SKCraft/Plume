# Plume

Plume provides a self-contained solution for Forge 1.7.10 that makes running servers easier.

## Modules

* Anytime Sleep
* BackTrack
* Bans
* BeanShell
* Broadcast
* Chat Channels
* Claims
* Command Filter
* Fancy Name
* Game Modes
* Item Blacklist
* Mob Commands
* Mob Mode
* Nether Coords
* Operator Check
* Parties
* Player Invites
* Restarts
* Sensible Numbers
* Spawns
* Status
* Teleports
* Users
* View Inventory
* Welcome Message
* World Border

## Development

[JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) required. [IntelliJ IDEA](https://www.jetbrains.com/idea/) recommended.

Plume uses a module system where individual modules can be enabled or disabled, but the default modules — and the meat of Plume — require MySQL. In addition, only a whitelist mode is available at the moment, so please follow the instructions below to set yourself up.

1. Install [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) locally.
2. Users not familiar with MySQL can install [HeidiSQL](http://www.heidisql.com/).
3. Create a `plume_dev` user with `plume_dev` as the password. You can either give the user full access to the database, or preferrably grant read/write/manage access to `plume\_*` and read access to the table `mysql.proc`. This can be done in HeidiSQL in "User manager" under "Tools",
4. Create the databases `plume_data` and `plume_log`. In HeidiSQL, right click the server on the left, go to "Create new" and choose "Database". Use "utf8mb4_general_ci" as the collation.
5. Import the tables from `schema/` into their respective databases. In HeidiSQL, select "plume_data", go to "Load SQL file..", select "plume_data.sql", and then click the blue play button in the toolbar to execute the query. Do the same for "plume_log".
6. Run `./gradlew clean setupDecompWorkspace build`
7. In IntelliJ IDEA, open up the `build.gradle` file to create a new project for Plume.
8. Make sure to install the Lombok plugin (see IDEA's settings to install plugins, then click Browse Repositories) and then after IDEA restarts, enable "annotation processing" in settings (use the search box).
9. On the Gradle panel (it may be on the right), browse to Plume - > Tasks -> Other and double click "genIntellijRuns". When it completes, confirm to reload the workspace.
9. Run -> Edit Configurations, choose "Minecraft Server", and add `-Dfml.coreMods.load=com.skcraft.plume.asm.PlumePlugin` to VM options, followed by `nogui` to program arguments.

Plume currently only runs on the server. When you first run the server, you will have to modify `run/eula.txt` to accept the EULA. Connect to the server with a regular client (NOT the client provided within your IDE).

In addition, add an entry to the "group" table (in MySQL) with `*` for permissions AND `autoJoin` set to `1`. Once the server is started, use `/invite <your_name>` to whitelist yourself.

## License

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
