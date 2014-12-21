package ru.valle.router;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        try {
            String routerAddress;
            String login;
            String password;
            if (args.length == 0 || args[0] == null) {
                System.out.println("No router's address was set, trying \"192.168.1.1\"");
                routerAddress = "192.168.1.1";
            } else {
                routerAddress = args[0];
            }
            if (!routerAddress.toLowerCase(Locale.US).startsWith("http")) {
                routerAddress = "http://" + routerAddress;
            }
            if (!routerAddress.endsWith("/")) {
                routerAddress += "/";
            }
            if (args.length <= 1 || args[1] == null) {
                System.out.println("No username set, trying \"admin\"");
                login = "admin";
            } else {
                login = args[1];
            }
            if (args.length <= 2 || args[2] == null) {
                System.out.println("No password set, trying \"admin\"");
                password = "admin";
            } else {
                password = args[2];
            }
            try {
                HttpURLConnection connectionLoc = (HttpURLConnection) new URL(routerAddress + "HNAP1/").openConnection();
                int httpCode = connectionLoc.getResponseCode();
                String apiResponse = new String(readAll(connectionLoc.getInputStream()), "UTF-8");
                if (httpCode != 200 || !apiResponse.contains("HNAP1")) {
                    System.err.println("No HNAP1 interface found for " + routerAddress + "HNAP1/");
                    return;
                }
            } catch (Exception e) {
                System.err.println("No HNAP1 interface found for " + routerAddress + "HNAP1/ because of " + e);
                return;
            }


            Enumeration<NetworkInterface> nInterfaces = NetworkInterface.getNetworkInterfaces();
            HashSet<String> macAddresses = new HashSet<String>();
            while (nInterfaces.hasMoreElements()) {
                Enumeration<InetAddress> inetAddresses = nInterfaces.nextElement().getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    String address = inetAddresses.nextElement().getHostAddress();
                    NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
                    if (network.isUp()) {
                        byte[] mac = network.getHardwareAddress();
                        if (mac != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < mac.length; i++) {
                                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                            }
                            String macAddress = sb.toString();
                            if (macAddresses.add(macAddress)) {
                                System.out.println(address + " -> " + macAddress + " " + network.getDisplayName());
                                try {
                                    allowAccessForThisMac(routerAddress, macAddress, login, password);
                                } catch (Exception e) {
                                    System.err.println("failed to allow access for  " + network);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void allowAccessForThisMac(String routerAddress, String macAddress, String login, String password) throws IOException {
        URL url = new URL(routerAddress + "HNAP1/");
        HttpURLConnection connectionLoc = (HttpURLConnection) url.openConnection();
        connectionLoc.setRequestMethod("POST");
        connectionLoc.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        connectionLoc.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((login + ":" + password).getBytes("UTF-8")));
        connectionLoc.setRequestProperty("SOAPAction", "http://cisco.com/HNAPExt/HotSpot/AddWebGUIAuthExemption");
        connectionLoc.setDoOutput(true);
        OutputStream os = connectionLoc.getOutputStream();
        os.write(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "<soap:Body>\n" +
                "<AddWebGUIAuthExemption xmlns=\"http://cisco.com/HNAPExt/HotSpot/\">\n" +
                "<MACAddress>" + macAddress + "</MACAddress>\n" +
                "<Duration>10</Duration>\n" +
                "</AddWebGUIAuthExemption>\n" +
                "</soap:Body>\n" +
                "</soap:Envelope>\n").getBytes("UTF-8"));
        os.close();
        int httpCode = connectionLoc.getResponseCode();
        System.out.println("HTTP " + httpCode);
        System.out.println(new String(readAll(connectionLoc.getInputStream()), "UTF-8"));
    }

    public static byte[] readAll(InputStream is) throws IOException {
        if (is != null) {
            byte[] buf = new byte[1024];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                while (true) {
                    int bytesReadCurr = is.read(buf);
                    if (bytesReadCurr == -1) {
                        baos.close();
                        byte[] data = baos.toByteArray();
                        baos = null;
                        return data;
                    } else if (bytesReadCurr > 0) {
                        baos.write(buf, 0, bytesReadCurr);
                    }
                }
            } finally {
                if (baos != null) {
                    baos.close();
                }
            }
        } else {
            return null;
        }
    }


}

