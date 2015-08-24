# Plume

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

## Architecture

### Lifecycle

1. Plume creates instances of all enabled module classes.
2. `InitializationEvent` is fired. Services (explained below) are to be registered here. Configuration is also loaded and saved to disk.
3. `PostInitializationEvent` is fired. Use of requested services can happen here.
4. `FMLPreInitializationEvent` is fired.
5. `FMLServerStartingEvent` is fired.
6. `FMLServerStartedEvent` is fired.

### Modules

Modules can be annotated with `@Module`:

```java
@Module(name = "example")
public class ExampleModule {
}
```

* Modules are normally automatically loaded on start.
* Modules can be enabled or disabled in the `config/plume/modules.cfg` file.

Some features (such as event bus registration) can be activated with `@AutoRegister` instead of `@Module`. Modules can be auto-loaded at boot time, but `@AutoRegister` classes aren't loaded unless some other class requests one.

### Event Handling

**Works for classes with `@Module` or `@AutoRegister`.**

Modules are subscribed against the FML and Forge event buses, as well as the Plume event bus.

To listen for FML state events, use `@Subscribe` from WorldEdit.

```java
@Module(name = "example")
public class ExampleModule {
    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
	}

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
    }
}
```

### Injection

**Works for all classes that are auto-injected, including modules.**

All classes (module or not) are loaded using the Guice library, which allows automatic injection of dependent classes:

```java
public class ExampleModule {
	@Inject
	private OtherClass otherClass;
}
```

Or if constructor injection is preferred:

```java
public class ExampleModule {
	@Inject
	public ExampleModule(OtherClass otherClass) {
	}
}
```

The dependent classes themselves are injected, so they too can contain `@Inject` and so on.

Note: Classes without `@Module`, `@AutoRegister`, or `@Singleton` will be instanciated for every time an instance is requested. For example, if two classes request a shared object, both classes will get two different instances.

### Config Injection

**Works for all classes that are auto-injected, including modules.**

Configuration can be written as a class:

```java
public class ClaimConfig {

    @Setting("cost")
    public Cost cost = new Cost();

    @ConfigSerializable
    public static class Cost {

        @Setting(comment = "The number of chunks that a user may own for free before claims have a cost")
        public int gratisChunkCount = 25;

        @Setting(comment = "The item required to buy a chunk")
        public SingleItemMatcher item = Inventories.typeDamageMatcher(new ItemStack(Items.coal));

	}
```

And then injected:

```java
public class Claims {
    @InjectConfig("claims") private Config<ClaimConfig> config;
}
```

The parameter for `@InjectConfig` is the filename for the configuration file. If multiple classes use the same configuration and file, then every class will have the same instance of `Config<?>`.

Configuration is available when `InitializationEvent` is fired (after the `VERY_EARLY` priority) and afterwards. Changes made to the configuration will be saved to disk in `InitializationEvent` as well, in the `VERY_LATE` priority.

### Service Locator

**Works for all classes that are auto-injected, including modules.**

Services can be registered in Plume's `InitializationEvent` (use `@Subscribe`):

```java
services.register(BanManager.class, bans);
```

In this case, `BanManager` is an interface and `bans` refers to an implementation.

Another class that needs this service can request it:

```java
@InjectService
private Service<BanManager> bans;
```

All requested services are currently required. Some time between `InitializationEvent` and `PostInitializationEvent`, Plume will check to make sure that all services requested exist. If not, loading will abort.

### Command Registration

**Works for classes with `@Module` or `@AutoRegister`.**

Commands can be registered easily with the `@Command` annotation, and sub-commands are supported.

Below is an example of `/debug tell <name> <age>`. The parameters to the method determine the parameters of the command.

```java
@Command(aliases = "tell", desc = "Tell a sender someone's age")
@Group(@At("debug"))
@Require("plume.party.create")
public void create(@Sender EntityPlayer sender, String name, int age) {
	sender.addChatMessage(Messages.info(name + " is " + age));
}
```

### Localization

Java's resource bundles are used. Localization keys are stored in `Plume.properties`.

Localization can be done with:

```java
tr("claims.message", arg1, arg2, arg3)
```

### Useful Helper Classes

```java
// Run things in the main thread
@Inject private TickExecutorService tickExecutorService;

// Get the server ID (defined in the config) with environment.getServerId()
@Inject private Environment environment;

// Useful things for running commands in the background
@Inject private BackgroundExecutor executor;

// Convert names to UUIDs
@Inject private ProfileService profileService;
```

### Deferred

Plume contains a simple API for working with ListenableFutures, which comes in handy when tasks must be done in the background, and then eventually the routine has to return to the main thread:

```java
Deferred<?> deferred = Deferreds
        .when(() -> {
			// This would block due to the HTTP request
            UserId userId = profileService.findUserId(name);

            return userId; // Passed onto the next handler
        }, executor.getExecutor()) // The background executor
        .done(userId -> {
            sender.addChatMessage(Messages.info("Got " + userId));
        }, tickExecutorService) // Run in the main thread
        .fail(e -> {
            if (e instanceof ProfileNotFoundException) {
                sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
            } else if (e instanceof ProfileLookupException) {
                sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
            } else {
                sender.addChatMessage(Messages.exception(e));
            }
        }, tickExecutorService); // Run in the main thread

// If the task takes longer than ~500ms, 
// a "please wait, processing" message gets sent
executor.notifyOnDelay(deferred, sender);
```
