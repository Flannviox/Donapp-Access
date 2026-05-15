# Feature: Publicar producto y lote

## 1. Descripción

Esta feature permite a un comerciante autenticado publicar un producto en el catálogo global de la aplicación y, en un segundo paso, asociar un lote a ese producto (cantidad, fecha de vencimiento, precio normal y precio de oferta opcional). El flujo está implementado como un único Fragment con dos vistas controladas por visibilidad. El estado del lote (`'en_oferta'` o `'disponible'`) se calcula de forma automática en el ViewModel a partir de la presencia del precio de oferta, sin intervención del usuario.

## 2. Archivos creados

Todos los archivos siguen el patrón arquitectónico ya establecido en `features/auth/` (Repository + HiltViewModel + StateFlow + DTOs).

```
app/src/main/java/com/grupo3/donapp_access/features/lotes/
├── dto/
│   ├── CategoriaDTO.kt
│   ├── ProductoDTO.kt
│   └── LoteDTO.kt
├── LoteRepository.kt
├── PublicarViewModel.kt
└── ui/
    └── PublicarLoteFragment.kt

app/src/main/res/layout/
└── fragment_publicar_lote.xml
```

Total: 7 archivos.

## 3. Archivos NO modificados

Durante el desarrollo de esta feature no se modificó ningún archivo existente. Se enumeran de forma explícita para evitar confusiones durante la revisión y para facilitar la integración paralela de otros integrantes del equipo:

- `features/auth/ui/HomeFragment.kt`
- `features/auth/ui/LoginFragment.kt`
- `features/auth/ui/RoleSelectionFragment.kt`
- `features/auth/AuthRepository.kt`
- `features/auth/AuthViewModel.kt`
- `features/auth/RegisterViewModel.kt`
- `features/auth/dto/UsuarioDTO.kt`
- `core/network/SupabaseClient.kt`
- `RegisterUserFragment.kt`
- `RegisterSellerFragment.kt`
- `DonappApplication.kt`
- `MainActivity.kt`
- `app/build.gradle.kts`
- `build.gradle.kts` (raíz)
- `gradle/libs.versions.toml`
- `res/layout/fragment_home.xml`
- `res/values/strings.xml`
- `res/values/colors.xml`
- `AndroidManifest.xml`

## 4. Cambios en la base de datos

Los siguientes cambios se ejecutaron manualmente en el SQL Editor del Dashboard de Supabase. El script es idempotente.

```sql
-- ════════════════════════════════════════════════════════════════════
-- DONAPP — Provisioning para feature "Publicar producto + lote"
-- ════════════════════════════════════════════════════════════════════

-- (a) Bucket "productos": público, 5 MB, solo imágenes jpeg/png/webp
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'productos',
    'productos',
    true,
    5242880,
    ARRAY['image/jpeg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO UPDATE
    SET public             = EXCLUDED.public,
        file_size_limit    = EXCLUDED.file_size_limit,
        allowed_mime_types = EXCLUDED.allowed_mime_types;


-- (b) RLS Policies

-- categoria: lectura para usuarios autenticados
DROP POLICY IF EXISTS "categoria_select_authenticated" ON public.categoria;
CREATE POLICY "categoria_select_authenticated"
    ON public.categoria FOR SELECT TO authenticated
    USING (true);

-- productos: catálogo global, lectura y escritura para autenticados
DROP POLICY IF EXISTS "productos_select_authenticated" ON public.productos;
CREATE POLICY "productos_select_authenticated"
    ON public.productos FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "productos_insert_authenticated" ON public.productos;
CREATE POLICY "productos_insert_authenticated"
    ON public.productos FOR INSERT TO authenticated
    WITH CHECK (true);

-- lote: lectura para todos; insert solo si tiendas_id pertenece al usuario
DROP POLICY IF EXISTS "lote_select_authenticated" ON public.lote;
CREATE POLICY "lote_select_authenticated"
    ON public.lote FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "lote_insert_dueno_tienda" ON public.lote;
CREATE POLICY "lote_insert_dueno_tienda"
    ON public.lote FOR INSERT TO authenticated
    WITH CHECK (
        tiendas_id IN (
            SELECT id_tienda
            FROM public.tiendas
            WHERE usuarios_id = auth.uid()
        )
    );

-- storage.objects bucket "productos":
--   SELECT: lectura pública (URLs directas para los clientes)
--   INSERT: solo dentro de la carpeta {id_tienda}/ del usuario logueado
DROP POLICY IF EXISTS "productos_bucket_select_public" ON storage.objects;
CREATE POLICY "productos_bucket_select_public"
    ON storage.objects FOR SELECT TO public
    USING (bucket_id = 'productos');

DROP POLICY IF EXISTS "productos_bucket_insert_dueno_tienda" ON storage.objects;
CREATE POLICY "productos_bucket_insert_dueno_tienda"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'productos'
        AND (storage.foldername(name))[1] IN (
            SELECT id_tienda::text
            FROM public.tiendas
            WHERE usuarios_id = auth.uid()
        )
    );


-- (c) Seed de 8 categorías iniciales
INSERT INTO public.categoria (nombre)
SELECT v.nombre
FROM (VALUES
    ('Frutas y verduras'),
    ('Panadería'),
    ('Lácteos'),
    ('Carnes y embutidos'),
    ('Abarrotes'),
    ('Bebidas'),
    ('Snacks y golosinas'),
    ('Otros')
) AS v(nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM public.categoria c WHERE c.nombre = v.nombre
);
```

## 5. Cómo navegar al Fragment

`PublicarLoteFragment` está diseñado para invocarse desde cualquier pantalla post-login en la que exista una sesión activa. Se sugiere validar previamente que el rol del usuario sea `Comerciante` (ver sección 6).

```kotlin
import com.grupo3.donapp_access.R
import com.grupo3.donapp_access.features.lotes.ui.PublicarLoteFragment

parentFragmentManager.beginTransaction()
    .setCustomAnimations(
        R.anim.slide_in_right,
        R.anim.slide_out_left,
        R.anim.slide_in_left,
        R.anim.slide_out_right
    )
    .replace(R.id.fragmentContainer, PublicarLoteFragment())
    .addToBackStack("publicar_lote")
    .commit()
```

## 6. Cómo leer el rol del usuario autenticado

El valor canónico del rol comerciante es la cadena exacta `"Comerciante"` (con mayúscula inicial, sin tilde), según el `CHECK` definido en la columna `usuarios.rol`.

```kotlin
import com.grupo3.donapp_access.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
private data class RolRow(val rol: String)

// Invocar dentro de viewLifecycleOwner.lifecycleScope.launch { ... }
suspend fun leerRolUsuarioActual(): String? {
    val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return null
    return SupabaseClient.client.from("usuarios")
        .select(columns = Columns.list("rol")) {
            filter { eq("id_usuarios", uid) }
            limit(1)
        }
        .decodeSingleOrNull<RolRow>()
        ?.rol
}
```

## 7. SQL para crear un comerciante de prueba

Antes de probar el flujo end-to-end es necesario disponer de un usuario con rol `Comerciante` y de una tienda asociada. El siguiente script debe ejecutarse en dos pasos.

```sql
-- ════════════════════════════════════════════════════════════════════
-- Crear comerciante de prueba con tienda
-- ════════════════════════════════════════════════════════════════════

-- ─── PASO 1 (manual, en el Dashboard) ────────────────────────────────
-- Authentication → Users → "Add user" → "Create new user"
--   Email:             test-comerciante@donapp.local
--   Password:          TestComer123!
--   Auto-confirm user: marcar la casilla
-- Copiar el UID que aparece tras crear el usuario.
-- ────────────────────────────────────────────────────────────────────


-- ─── PASO 2 (SQL Editor) ─────────────────────────────────────────────
-- Reemplazar el placeholder con el UID del paso 1 y ejecutar.
DO $$
DECLARE
    v_auth_uid uuid := 'REEMPLAZAR_CON_EL_UID_DEL_PASO_1'::uuid;
BEGIN
    -- Perfil en public.usuarios (id_usuarios = mismo uuid que auth.users.id)
    INSERT INTO public.usuarios (
        id_usuarios,
        nombres,
        apellidos,
        correo,
        dni,
        rol,
        tipo_discapacidad,
        correo_apoderado,
        telefono
    ) VALUES (
        v_auth_uid,
        'Test',
        'Comerciante',
        'test-comerciante@donapp.local',
        '88888888',
        'Comerciante',
        NULL,
        NULL,
        '999888777'
    );

    -- Tienda asociada (coordenadas dummy del centro de Lima)
    INSERT INTO public.tiendas (
        usuarios_id,
        nombre,
        direccion,
        referencia,
        latitud,
        longitud,
        rating_promedio,
        hora_atencion
    ) VALUES (
        v_auth_uid,
        'Bodega de Prueba',
        'Av. Test 123, Lima',
        'Esquina con Av. Demo',
        -12.0464,
        -77.0428,
        0.0,
        'Lun-Sáb 8:00-20:00'
    );
END $$;


-- Verificación: debe devolver una fila con la tienda enlazada
SELECT
    u.nombres,
    u.rol,
    u.correo,
    t.id_tienda,
    t.nombre AS tienda
FROM public.usuarios u
LEFT JOIN public.tiendas t ON t.usuarios_id = u.id_usuarios
WHERE u.correo = 'test-comerciante@donapp.local';
```

### Notas sobre `auth.users`

No es posible insertar filas en `auth.users` directamente desde SQL. La fila requiere campos generados internamente por el servicio de autenticación (`encrypted_password`, `aud`, `instance_id`, entre otros). Por ese motivo el paso 1 debe realizarse a través de la interfaz del Dashboard o del flujo de registro de la propia aplicación.

## 8. Estado de entrega

- **Compilación**: `BUILD SUCCESSFUL`. Verificado con `gradlew.bat :app:assembleDebug`. Sin warnings nuevos atribuibles a esta feature.
- **Cobertura funcional**: completa según los requerimientos definidos.
- **Pendiente externo**: integración desde la pantalla post-login. Esta tarea corresponde al integrante responsable del flujo de autenticación. El presente documento incluye los fragmentos de código necesarios en las secciones 5 y 6.
- **Pendiente operativo**: provisión de datos de prueba en la base de datos según el script de la sección 7. Una vez creados el usuario comerciante y la tienda asociada, la feature queda lista para validación funcional.

---

*Documento generado durante el desarrollo de la feature de lotes. Para consultas dirigirse al responsable del módulo `features/lotes/`.*
