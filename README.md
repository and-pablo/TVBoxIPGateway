# TVBox IP Gateway

App sencilla para TV Box / Android TV que muestra:

- **IP Local**
- **IP Pública**
- **Gateway**
- **MAC LAN (Ethernet)**
- **MAC WIFI**

## Cómo generar el APK

1. Sube **todos** estos archivos a la raíz de tu repositorio de GitHub.
2. Ve a la pestaña **Actions**.
3. Ejecuta el workflow **Build APK**.
4. Cuando termine en verde, descarga el artefacto **TVBox-IP-Gateway-APK**.
5. Dentro está el `app-debug.apk`.

## Notas

- La IP Pública se obtiene de internet (necesita conexión).
- Las MAC pueden mostrar "No disponible" en algunos dispositivos por restricciones de Android.
- Funciona mejor en TV Boxes con Ethernet.
