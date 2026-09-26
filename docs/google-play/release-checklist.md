# Checklist de publicación en Google Play

## Cuenta y ficha

- [ ] Verificar identidad, dirección, teléfono y correo de la cuenta de desarrollador.
- [ ] Registrar el package `com.gabow95k.keeply` en Play Console.
- [ ] Definir correo público de soporte.
- [ ] Publicar `privacy-policy.html` en una URL HTTPS pública, estable, sin login y no editable.
- [ ] Pegar nombre, descripciones y notas desde los archivos de esta carpeta.
- [ ] Categoría: `Casa y hogar`; declarar que no es una app médica.
- [ ] Anuncios: `No contiene anuncios`.
- [ ] Acceso a la app: `Todas las funciones están disponibles sin acceso especial`.
- [ ] Público objetivo recomendado: `18 años o más`; confirmar que coincide con la estrategia real.
- [ ] Completar IARC: sin violencia, sexo, lenguaje, apuestas, UGC ni interacción entre usuarios.

## Seguridad de los datos y políticas

- [ ] Completar Seguridad de los datos según `data-safety.md`.
- [ ] Declarar permisos de cámara y notificaciones en App content cuando Play Console lo solicite.
- [ ] Confirmar que no se seleccionó Families/niños salvo que se implemente todo lo requerido.
- [ ] Revisar que el correo en la política sea el mismo de la ficha.

## Binario

- [x] `targetSdk = 36`, requerido desde el 31 de agosto de 2026.
- [x] `versionCode = 1`, `versionName = 1.0` para primera publicación.
- [x] App Bundle de release compila.
- [ ] Crear y respaldar una clave de carga privada; nunca versionarla.
- [ ] Configurar `keystore.properties` local y generar un AAB firmado.
- [ ] Activar Play App Signing.
- [ ] Subir primero a prueba interna/cerrada y ejecutar el informe previo al lanzamiento.
- [ ] Atender crashes, ANR, problemas de accesibilidad y compatibilidad del informe.

## Recursos gráficos

- [x] Ícono Play: PNG 512 × 512 con alfa, menos de 1 MB.
- [x] Gráfico destacado: PNG 1024 × 500 sin alfa.
- [x] Seis capturas promocionales JPEG 1080 × 1920 (9:16, sin alfa), con texto breve y UI real.
- [x] Textos alternativos preparados en `store-listing-es.md`.
- [ ] Revisar visualmente una última vez los archivos finales en Play Console.

## Pruebas finales externas

- [ ] Cámara y OCR en al menos un dispositivo físico.
- [ ] Escáner EAN-8, EAN-13, UPC-A y UPC-E con Google Play services actualizado.
- [ ] Notificaciones permitidas y denegadas en Android 13–16.
- [ ] Instalación limpia, actualización y conservación de datos desde una versión previa real.
- [ ] Prueba de accesibilidad con TalkBack y tamaño de fuente 200%.
