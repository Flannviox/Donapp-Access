# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Native Android app (Kotlin) called **Donapp-Access**. Single Gradle module: `:app`. Package: `com.grupo3.donapp_access`. `compileSdk = 36`, `minSdk = 24`, JVM target 11.

## Build & run

Gradle wrapper is included. Use `gradlew.bat` on Windows (PowerShell).

- Build debug APK: `./gradlew.bat :app:assembleDebug`
- Install on connected device/emulator: `./gradlew.bat :app:installDebug`
- Unit tests (JVM): `./gradlew.bat :app:testDebugUnitTest`
- Single unit test: `./gradlew.bat :app:testDebugUnitTest --tests "com.grupo3.donapp_access.SomeTest.someMethod"`
- Instrumented tests (requires device/emulator): `./gradlew.bat :app:connectedDebugAndroidTest`
- Lint: `./gradlew.bat :app:lintDebug`
- Clean: `./gradlew.bat clean`

Note: `app/src/test` and `app/src/androidTest` exist but are empty scaffolds — there are no actual tests yet.

## Required secrets in `local.properties`

The project will compile without these but auth/maps will not work at runtime. They are injected as `BuildConfig` fields at build time (see `app/build.gradle.kts`):

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `MAPBOX_TOKEN` — public runtime token, also wired into the manifest as `MAPBOX_ACCESS_TOKEN` placeholder and consumed by `MainActivity` via `MapboxOptions.accessToken = BuildConfig.MAPBOX_TOKEN`.
- `MAPBOX_SECRET_TOKEN` — **download** token. Read in `settings.gradle.kts` as a `Bearer` credential for the Mapbox Maven repo. Without it, dependency resolution for Mapbox fails.

`local.properties` is not committed; replace the empty values before building.

## Architecture

Feature-based package layout under `com.grupo3.donapp_access`:

- `DonappApplication` — `@HiltAndroidApp` entry; required for Hilt to generate the DI graph.
- `MainActivity` — `@AndroidEntryPoint`, single-activity host. Sets `R.layout.activity_main` (which contains `R.id.fragmentContainer`) and on cold start commits `RoleSelectionFragment` inside `window.decorView.post { ... }` (deferred to next UI loop tick — preserve this when modifying startup).
- `core/network/SupabaseClient` — singleton `object` that calls `createSupabaseClient(...)` once and installs the `Auth`, `Postgrest`, `Storage`, and `Realtime` modules. All Supabase access goes through `SupabaseClient.client`.
- `features/auth/`
  - `AuthRepository` (`@Inject constructor()`) — wraps `supabase.auth.signUpWith/signInWith(Email)` and inserts a `UsuarioDTO` row into the `usuarios` table via `SupabaseClient.client.from("usuarios").insert(...)`.
  - `AuthViewModel` (`@HiltViewModel`) — exposes `loginState` / `registerState` as `StateFlow<AuthState>` (sealed: `Idle`/`Loading`/`Success`/`Error`). `crearCuentaCompleta(...)` calls `signUp`, then reads `currentUserOrNull()?.id` and forwards it to `repository.registrarEnTablaUsuarios(...)`.
  - `RegisterViewModel` (`@HiltViewModel`) — shared across fragments via `by activityViewModels()`. Currently only holds `selectedRole` ("Cliente" / "Comerciante"), used to pass the role choice from `RoleSelectionFragment` to the register fragments.
  - `dto/UsuarioDTO` — `@Serializable` shape sent to Supabase. Uses `@SerialName` to map camelCase Kotlin fields to snake_case Postgres columns (`id_usuarios`, `tipo_discapacidad`, `correo_apoderado`). Update both sides together when adding columns.
  - `models/Usuario` — plain in-memory domain model. Kept separate from the DTO on purpose.
  - `ui/` — `RoleSelectionFragment`, `LoginFragment`, `HomeFragment`.
- Two register fragments (`RegisterUserFragment`, `RegisterSellerFragment`) currently live at the **root** package, not under `features/auth/ui/` — `RoleSelectionFragment` imports them from `com.grupo3.donapp_access`. If you move them, fix imports in `RoleSelectionFragment`.

### Navigation

Despite the `navigation-safeargs` Gradle plugin being applied and `navigation-fragment-ktx` being on the classpath, fragment navigation is done **manually** via `parentFragmentManager.beginTransaction().replace(R.id.fragmentContainer, ...).addToBackStack(...)` with custom anims (`R.anim.slide_in_right` etc.). There is no `nav_graph.xml` in use. Follow the same pattern when adding screens unless you are intentionally migrating to Navigation Component.

### Async / state

Repository functions are `suspend`; ViewModels invoke them inside `viewModelScope.launch { ... }` with try/catch that pushes failures into the `AuthState.Error(message)` flow. UI should `collect` the state flow rather than calling repository methods directly.

### View bindings

`viewBinding = true` is enabled. Some fragments use generated `Fragment*Binding` (e.g. `FragmentRegisterUserBinding.inflate(...)`); others use `view.findViewById(...)`. Either is acceptable — match the existing style of the file you are editing.

## Gradle / dependencies

Versions are centralized in `gradle/libs.versions.toml` (version catalog). Add new deps by editing that file and referencing them as `libs.<alias>` in `app/build.gradle.kts`. Hilt and Room use `kapt` (not KSP) — keep that consistent when adding annotation-processed libraries.

Key stack: Hilt 2.56, Supabase-kt 3.1.4 (postgrest/auth/storage/realtime), Ktor 3.1.3 (Supabase transport), Mapbox Maps 11.10, Room 2.7.1, Navigation 2.9, Coroutines 1.10.2, Glide 4.16, Play Services Location 21.3.