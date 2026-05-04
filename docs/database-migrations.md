# Database Migrations

`0.1.0` shipped with Room schema version `5`. User workout history now needs to be treated as durable app data.

## Current policy

- Do not use `fallbackToDestructiveMigration()` in production builds.
- Every Room schema change must increment `AppDatabase` version.
- Every Room schema version must be exported into `app/schemas/`.
- Every version bump must add an explicit migration to `DatabaseMigrations.ALL`.
- If a change cannot be migrated safely, stop and design the migration before release.

## Release workflow for schema changes

1. Modify entities, DAO contracts, or converters.
2. Increase `AppDatabase` version.
3. Add a `Migration(startVersion, endVersion)` in `DatabaseMigrations.kt`.
4. Run `assembleDebug` to generate the new schema JSON in `app/schemas/`.
5. Add or update a migration test that upgrades from the previous schema to the new one.
6. Only release after the migration path succeeds on an existing database.

## Notes for this project

- Baseline production schema: version `5`
- Database file name: `c25k.db`
- Local data currently includes plan progress, workout history, workout segments, and track points.
- Schema export is enabled so Room can validate future migration paths against checked-in JSON snapshots.
