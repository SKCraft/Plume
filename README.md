# Plume

## Development

1. Install MySQL server locally.
2. Create a `plume_dev` user with `plume_dev` as the password. Grant read/write/manage access to `plume\_*` and read access to `mysql.proc`.
3. Create the database schemas `plume_data` and `plume_log`.
4. Import the tables from schema/plum_data.sql into `plum_data`.
5. Run `./gradlew clean setupDecompWorkspace build`

### IntelliJ IDEA

Make sure to install the Lombok plugin and enable "annotation processing" (in the settings) for the project.