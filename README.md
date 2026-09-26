# Impulso Joven

Aplicación Android offline para que líderes del **Ministerio de Adolescentes y Jóvenes** registren y evalúen el compromiso de cada miembro durante un ciclo de 12 encuentros.

La puntuación valora asistencia, amor por la Palabra, adoración, participación, puntualidad, respeto y convivencia. La asistencia por sí sola aporta 20/100: el resto exige participación real.

## Funciones incluidas

- Acceso local protegido para el líder con contraseña cifrada mediante PBKDF2.
- Alta, edición, foto, archivo/reactivación y eliminación de miembros.
- Doce encuentros por miembro, con bloqueo automático de aspectos cuando está ausente.
- Cálculo automático de 100 puntos por encuentro y 1200 por ciclo.
- Trece penalizaciones acumulables, incluyendo conducta en oración, clase, alabanza y juegos; el resultado nunca baja de cero.
- Detalle de aspectos, penalizaciones, notas, creación y última modificación.
- Resumen, progreso, ranking y niveles configurables de recompensa.
- Reporte PDF, archivo CSV compatible con Excel y respaldo JSON.
- Historial local de cambios y operación completa sin Internet.
- Tema Material Design 3 moderno, adaptable a modo claro/oscuro, con ilustraciones vectoriales cristianas, encabezados juveniles y versículos de motivación en las pantallas principales.

## Arquitectura

MVVM con flujo unidireccional de estado:

```text
Compose UI → MinistryViewModel (StateFlow) → MinistryRepository → Room → SQLite
```

```text
app/src/main/java/com/ministerio/jovenes/
├── MainActivity.kt
├── MinisterioApp.kt
├── data/
│   ├── local/          # Entidades, DAO y AppDatabase
│   └── repository/     # Reglas, transacciones y modelos de dominio
├── ui/
│   ├── components/     # Componentes reutilizables
│   ├── screens/        # Login, inicio, miembros, registro, ranking, reportes, ajustes
│   ├── theme/          # Material 3 y paleta
│   ├── MinistryRoot.kt # Navigation Compose
│   └── MinistryViewModel.kt
└── util/               # PDF/CSV/JSON y hash de contraseña
```

## Base de datos Room

| Tabla | Propósito |
|---|---|
| `members` | Perfil, foto opcional, nacimiento, grupo y estado |
| `meetings` | Catálogo fijo de los 12 encuentros |
| `attendance_records` | Asistencia, notas y marcas de tiempo |
| `aspect_scores` | Resultado y puntos de cada uno de los 7 aspectos |
| `penalty_types` | Catálogo de penalizaciones y descuentos |
| `applied_penalties` | Penalizaciones aplicadas a un registro |
| `change_history` | Auditoría de altas, cambios y eliminaciones |
| `admin_users` | Usuario líder y hash seguro de contraseña |
| `app_settings` | Ministerio, ciclo y umbrales de premios |

Las operaciones que afectan varias tablas se ejecutan dentro de transacciones Room.

## Puntuación

| Aspecto | Puntos |
|---|---:|
| Asistencia | 15 |
| Atender y prestar atención a la clase | 10 |
| Leer la Palabra | 10 |
| Orar con reverencia | 10 |
| Alabar o adorar | 10 |
| Mantenerse de pie al alabar | 5 |
| Responder preguntas | 15 |
| Juegos: participa correctamente | 15 |
| Juegos: no desea jugar, pero permanece respetuoso | 5 |
| Puntualidad | 10 |
| **Máximo por encuentro** | **100** |

Si el miembro no asiste, los demás controles se desactivan y el resultado es 0. En juegos existe una valoración parcial de 5 puntos para quien no desea jugar pero permanece respetuoso. Las penalizaciones se descuentan después de sumar aspectos y el total se limita al rango 0–100.

## Abrir y ejecutar en Android Studio

Requisitos recomendados:

- Android Studio Ladybug o posterior.
- JDK 17 (el JDK integrado de Android Studio sirve).
- Android SDK 35.

1. Clona el repositorio.
2. En Android Studio selecciona **Open** y abre la carpeta raíz.
3. Espera la sincronización de Gradle.
4. Selecciona un emulador o teléfono con Android 8.0 (API 26) o superior.
5. Pulsa **Run app**.

Primer acceso:

```text
Usuario: admin
Contraseña: Admin123!
```

Cambia la contraseña inmediatamente en **Ajustes → Seguridad del líder**. Los datos se almacenan únicamente en el dispositivo.

## Compilar la APK localmente

Linux/macOS:

```bash
chmod +x gradlew
./gradlew testDebugUnitTest assembleRelease
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease
```

APK resultante:

```text
app/build/outputs/apk/release/app-release.apk
```

## GitHub Actions y descarga del APK

El workflow [`.github/workflows/android.yml`](.github/workflows/android.yml) se ejecuta en cada `push`, `pull_request` hacia `main` o manualmente. Configura Java 17, prueba el proyecto, ejecuta `./gradlew assembleRelease` y publica la APK release firmada por 30 días.

Para descargarla:

1. Abre el repositorio en GitHub.
2. Entra en **Actions**.
3. Selecciona **Compilar APK Android** y una ejecución exitosa.
4. Baja hasta **Artifacts**.
5. Descarga `impulso-joven-release-<número>` y descomprime el ZIP.

La APK release generada usa una firma interna para instalación directa. Para publicar actualizaciones en Google Play se debe sustituir por una clave privada de producción almacenada como GitHub Secret; nunca subas archivos `.jks` ni contraseñas al repositorio.

## Exportaciones y respaldo

En **Reporte final**:

- **PDF:** ranking, puntaje y reconocimiento, listo para imprimir.
- **CSV / Excel:** resumen y detalle por encuentro.
- **JSON:** copia portable de los datos relevantes del ciclo.

Android muestra el selector del sistema para elegir dónde guardar cada archivo. La copia automática de Android también está habilitada para base de datos y preferencias.

## Privacidad y buenas prácticas

Las fotos se referencian mediante URI persistente del selector de documentos; no se duplican. Evita compartir reportes con datos de menores sin autorización. Archiva un miembro si deseas conservar sus registros; eliminarlo borra sus evaluaciones en cascada.
