package com.tvbox.ipgateway;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private TextView tvPublica;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 30, 40, 30);
        layout.setBackgroundColor(Color.BLACK);

        TextView title = crearTexto("DATOS DE RED", 30, Color.parseColor("#00E676"));
        layout.addView(title);

        layout.addView(crearTexto("IP LOCAL: " + obtenerIPLocal(), 24, Color.WHITE));
        tvPublica = crearTexto("IP PUBLICA: cargando...", 24, Color.WHITE);
        layout.addView(tvPublica);
        layout.addView(crearTexto("GATEWAY: " + obtenerGateway(), 24, Color.WHITE));
        layout.addView(crearTexto("MAC LAN: " + obtenerMacEthernet(), 24, Color.WHITE));
        layout.addView(crearTexto("MAC WIFI: " + obtenerMacWifi(), 24, Color.WHITE));

        setContentView(layout);

        // Cargar IP pública en segundo plano
        cargarIpPublica();
    }

    private TextView crearTexto(String texto, int tamaño, int color) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(color);
        tv.setTextSize(tamaño);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 14, 0, 14);
        return tv;
    }

    private void cargarIpPublica() {
        executor.execute(() -> {
            String ip = obtenerIpPublica();
            mainHandler.post(() -> {
                if (tvPublica != null) {
                    tvPublica.setText("IP PUBLICA: " + ip);
                }
            });
        });
    }

    private String obtenerIpPublica() {
        String[] servicios = {
                "https://api.ipify.org",
                "https://icanhazip.com",
                "https://ifconfig.me/ip"
        };
        for (String servicio : servicios) {
            try {
                URL url = new URL(servicio);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = reader.readLine();
                reader.close();
                conn.disconnect();
                if (line != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        return line;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "No disponible";
    }

    private String obtenerIPLocal() {
        try {
            for (NetworkInterface interfaz : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nombre = interfaz.getName().toLowerCase();
                // Preferir eth / en / wlan
                if (!(nombre.startsWith("eth") || nombre.startsWith("en") || nombre.startsWith("wlan") || nombre.contains("wifi"))) {
                    continue;
                }
                for (InetAddress direccion : Collections.list(interfaz.getInetAddresses())) {
                    if (!direccion.isLoopbackAddress() && direccion instanceof Inet4Address) {
                        return direccion.getHostAddress();
                    }
                }
            }
            // Fallback: cualquier IPv4 no loopback
            for (NetworkInterface interfaz : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress direccion : Collections.list(interfaz.getInetAddresses())) {
                    if (!direccion.isLoopbackAddress() && direccion instanceof Inet4Address) {
                        return direccion.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "No disponible";
    }

    private String obtenerGateway() {
        // Método 1: comando ip route (funciona en muchos TV Box)
        try {
            Process proceso = Runtime.getRuntime().exec(new String[]{"ip", "route"});
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.startsWith("default via ")) {
                    String[] partes = linea.split("\\s+");
                    if (partes.length >= 3) {
                        return partes[2];
                    }
                }
            }
            lector.close();
        } catch (Exception ignored) {
        }

        // Método 2: getprop (algunos dispositivos)
        try {
            Process proceso = Runtime.getRuntime().exec(new String[]{"getprop", "dhcp.eth0.gateway"});
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea = lector.readLine();
            lector.close();
            if (linea != null && !linea.trim().isEmpty() && !linea.contains("null")) {
                return linea.trim();
            }
        } catch (Exception ignored) {
        }

        try {
            Process proceso = Runtime.getRuntime().exec(new String[]{"getprop", "dhcp.wlan0.gateway"});
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea = lector.readLine();
            lector.close();
            if (linea != null && !linea.trim().isEmpty() && !linea.contains("null")) {
                return linea.trim();
            }
        } catch (Exception ignored) {
        }

        return "No disponible";
    }

    private String obtenerMacEthernet() {
        String[] posibles = {"eth0", "eth1", "eth2", "en0", "en1", "enp0s3", "ens33"};
        for (String nombre : posibles) {
            String mac = obtenerMacInterfaz(nombre);
            if (esMacValida(mac)) {
                return mac;
            }
        }
        try {
            for (NetworkInterface interfaz : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nombre = interfaz.getName().toLowerCase();
                if (nombre.startsWith("eth") || nombre.startsWith("en")) {
                    String mac = obtenerMac(interfaz);
                    if (esMacValida(mac)) {
                        return mac;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "No disponible";
    }

    private String obtenerMacWifi() {
        String[] posibles = {"wlan0", "wlan1", "wifi0", "wlan2"};
        for (String nombre : posibles) {
            String mac = obtenerMacInterfaz(nombre);
            if (esMacValida(mac)) {
                return mac;
            }
        }
        try {
            for (NetworkInterface interfaz : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nombre = interfaz.getName().toLowerCase();
                if (nombre.startsWith("wlan") || nombre.contains("wifi")) {
                    String mac = obtenerMac(interfaz);
                    if (esMacValida(mac)) {
                        return mac;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "No disponible";
    }

    private String obtenerMacInterfaz(String nombre) {
        try {
            NetworkInterface interfaz = NetworkInterface.getByName(nombre);
            if (interfaz != null) {
                return obtenerMac(interfaz);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String obtenerMac(NetworkInterface interfaz) {
        try {
            byte[] mac = interfaz.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                return null;
            }
            StringBuilder resultado = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i > 0) {
                    resultado.append(":");
                }
                resultado.append(String.format("%02X", mac[i] & 0xFF));
            }
            return resultado.toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean esMacValida(String mac) {
        if (mac == null) {
            return false;
        }
        // Android 6+ a veces devuelve esta MAC falsa
        if (mac.equals("02:00:00:00:00:00") || mac.equals("00:00:00:00:00:00")) {
            return false;
        }
        return mac.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
