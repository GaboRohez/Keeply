# Declaración sugerida de Seguridad de los datos

Revisada contra el código y dependencias incluidas el 24 de septiembre de 2026. Las respuestas finales deben coincidir con la versión exacta del AAB enviado.

## Resumen para Play Console

- ¿La app recopila o comparte datos de usuario requeridos por la sección? **Sí, recopila datos técnicos mediante ML Kit.**
- ¿Comparte datos con terceros? **No** según la documentación de ML Kit; Google procesa los datos del SDK como proveedor de servicio.
- ¿Cifra los datos en tránsito? **Sí**, ML Kit declara HTTPS.
- ¿Los usuarios pueden solicitar eliminación? **No aplica para cuentas**, porque Keeply no crea cuentas. Los datos locales se eliminan desde la app cuando existe la acción correspondiente, desde Ajustes de Android al borrar datos o al desinstalar.
- ¿La app permite crear una cuenta? **No**.

## Datos que se deben declarar como recopilados

| Tipo de Google Play | Recopilado | Compartido | Obligatorio | Finalidad |
|---|---:|---:|---:|---|
| Información de la app y rendimiento → Diagnósticos | Sí | No | Sí, por el SDK | Diagnóstico, seguridad y mejora del SDK |
| Dispositivo u otros identificadores | Sí | No | Sí, por el SDK | Diagnóstico y analítica técnica del SDK |

ML Kit documenta que puede recopilar fabricante, modelo, versión/build del sistema, aceleradores disponibles, package/versiones de la app, identificadores por instalación y métricas de uso. Si se activa auto-zoom en el futuro, hay datos técnicos adicionales de la sesión de escaneo; la versión actual de Keeply no llama `enableAutoZoom()`.

## Datos utilizados solo en el dispositivo

No se marcan como “recopilados” en Play Console mientras la implementación siga siendo local y no los envíe fuera del dispositivo:

- Nombre, edad, tipo de sangre, teléfono, correo y notas de perfil.
- Inventario, cantidades, mínimos, caducidades, códigos, ubicaciones y notas.
- Fotos del producto —tomadas con cámara o elegidas individualmente con el selector del sistema— y texto/códigos extraídos de ellas. La app no solicita acceso general a la galería.
- Listas de compra e historial de movimientos.
- Preferencias y aceptación del aviso.

## Verificaciones antes de enviar cada versión

1. Revisar el informe de SDKs de Play Console después de subir el AAB.
2. Confirmar que no se añadieron analítica, crash reporting, publicidad, nube o respaldo.
3. Actualizar esta declaración y la política si cambia cualquier dependencia o flujo.
4. Conservar la distinción: el contenido procesado por ML Kit permanece local, pero el SDK envía métricas técnicas.

Fuentes oficiales: [divulgación de datos de ML Kit](https://developers.google.com/ml-kit/android-data-disclosure)
y [formulario de Seguridad de los datos de Google Play](https://support.google.com/googleplay/android-developer/answer/10787469?hl=es-419).
