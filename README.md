# Plume

## Compiling

1. Install MySQL server locally.
2. Create a `plume_dev` user with `plume_dev` as the password. Grant read/write/manage access to `plume\_*` and read access to `mysql.proc`.
3. Create the database schemas `plume_data` and `plume_log`.
4. Import the tables from schema/plum_data.sql into `plum_data`.
5. Run `./gradlew clean setupDecompWorkspace build`

### IntelliJ IDEA

Make sure to install the Lombok plugin and enable "annotation processing" (in the settings) for the project.

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