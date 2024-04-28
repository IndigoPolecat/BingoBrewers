package com.github.indigopolecat.bingobrewers.util;

import com.github.indigopolecat.bingobrewers.BingoBrewers;
import com.github.indigopolecat.bingobrewers.BingoBrewersConfig;
import com.github.indigopolecat.bingobrewers.Hud.TitleHud;
import com.github.indigopolecat.bingobrewers.gui.UpdateScreen;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import moe.nea.libautoupdate.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.compress.utils.IOUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class AutoUpdater {
    public static boolean updateScreen = false;

    private final UpdateContext context = new UpdateContext(
            UpdateSource.githubUpdateSource("IndigoPolecat", "BingoBrewers"),
            UpdateTarget.deleteAndSaveInTheSameFolder(AutoUpdater.class),
            CurrentVersion.ofTag(BingoBrewers.version),
            "BingoBrewers"
    );

    public CompletableFuture<Boolean> checkUpdate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String updaterType = "none";
                if (BingoBrewersConfig.autoUpdaterType == 0) updaterType = "full";
                if (BingoBrewersConfig.autoUpdaterType == 1) updaterType = "pre";
                PotentialUpdate potentialUpdate = context.checkUpdate(updaterType).join();
                Thread.sleep(1000);
                return potentialUpdate.isUpdateAvailable();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Executors.newSingleThreadExecutor());
    }

    public CompletableFuture<Boolean> update() {
        String updaterType = "none";
        if (BingoBrewersConfig.autoUpdaterType == 0) updaterType = "full";
        if (BingoBrewersConfig.autoUpdaterType == 1) updaterType = "pre";
        return context.checkUpdate(updaterType).thenComposeAsync(potentialUpdate -> {
            if (potentialUpdate.isUpdateAvailable()) {
                return potentialUpdate.launchUpdate().thenApply((ignored) -> {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Bingo Brewers has been updated to the latest version! Please restart your game to apply the update."));
                    return true;
                });
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    static boolean updateChecked = false;

    public static boolean isThereUpdate = false;

    @SubscribeEvent
    public void updateCheck(WorldEvent event) {
        if (!(event instanceof WorldEvent.Load)) return;
        if (!updateChecked) {
            updateChecked = true;
            UpdateUtils.patchConnection(urlConnection -> {
                if (urlConnection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(getSSLSocketFactory());
                }
            });
            System.out.println("Checking for updates...");
            checkUpdate().thenAccept(updateAvailable -> {
                System.out.println("update available: " + updateAvailable);
                if (updateAvailable) {
                    if (BingoBrewersConfig.autoDownload) {
                        BingoBrewers.autoUpdater.update();
                        BingoBrewers.activeTitle = new TitleHud("Bingo Brewers will update on game close.", 0x47EB62, 4000);
                    } else {
                        isThereUpdate = true;
                        updateScreen = true;
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (updateScreen && event.phase == TickEvent.Phase.END) {
            updateScreen = false;
            Minecraft.getMinecraft().displayGuiScreen(new UpdateScreen());
        }
    }

    public String getChangelog() {
        try {
            URL url = new URL("https://api.github.com/repos/IndigoPolecat/BingoBrewers/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String input;
                StringBuilder response = new StringBuilder();

                while ((input = in.readLine()) != null) {
                    response.append(input);
                }
                in.close();

                Gson gson = new Gson();

                return gson.fromJson(response.toString(), JsonObject.class).get("body").getAsString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Could not fetch changelog";
    }

    public static SSLContext buildSslContext() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, KeyManagementException {
        X509Certificate cert;
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        String sslCert = "\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIEozCCBEmgAwIBAgIQTij3hrZsGjuULNLEDrdCpTAKBggqhkjOPQQDAjCBjzEL\n" +
                "MAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UE\n" +
                "BxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTcwNQYDVQQDEy5T\n" +
                "ZWN0aWdvIEVDQyBEb21haW4gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMB4X\n" +
                "DTI0MDMwNzAwMDAwMFoXDTI1MDMwNzIzNTk1OVowFTETMBEGA1UEAxMKZ2l0aHVi\n" +
                "LmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABARO/Ho9XdkY1qh9mAgjOUkW\n" +
                "mXTb05jgRulKciMVBuKB3ZHexvCdyoiCRHEMBfFXoZhWkQVMogNLo/lW215X3pGj\n" +
                "ggL+MIIC+jAfBgNVHSMEGDAWgBT2hQo7EYbhBH0Oqgss0u7MZHt7rjAdBgNVHQ4E\n" +
                "FgQUO2g/NDr1RzTK76ZOPZq9Xm56zJ8wDgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB\n" +
                "/wQCMAAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMEkGA1UdIARCMEAw\n" +
                "NAYLKwYBBAGyMQECAgcwJTAjBggrBgEFBQcCARYXaHR0cHM6Ly9zZWN0aWdvLmNv\n" +
                "bS9DUFMwCAYGZ4EMAQIBMIGEBggrBgEFBQcBAQR4MHYwTwYIKwYBBQUHMAKGQ2h0\n" +
                "dHA6Ly9jcnQuc2VjdGlnby5jb20vU2VjdGlnb0VDQ0RvbWFpblZhbGlkYXRpb25T\n" +
                "ZWN1cmVTZXJ2ZXJDQS5jcnQwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLnNlY3Rp\n" +
                "Z28uY29tMIIBgAYKKwYBBAHWeQIEAgSCAXAEggFsAWoAdwDPEVbu1S58r/OHW9lp\n" +
                "LpvpGnFnSrAX7KwB0lt3zsw7CAAAAY4WOvAZAAAEAwBIMEYCIQD7oNz/2oO8VGaW\n" +
                "WrqrsBQBzQH0hRhMLm11oeMpg1fNawIhAKWc0q7Z+mxDVYV/6ov7f/i0H/aAcHSC\n" +
                "Ii/QJcECraOpAHYAouMK5EXvva2bfjjtR2d3U9eCW4SU1yteGyzEuVCkR+cAAAGO\n" +
                "Fjrv+AAABAMARzBFAiEAyupEIVAMk0c8BVVpF0QbisfoEwy5xJQKQOe8EvMU4W8C\n" +
                "IGAIIuzjxBFlHpkqcsa7UZy24y/B6xZnktUw/Ne5q5hCAHcATnWjJ1yaEMM4W2zU\n" +
                "3z9S6x3w4I4bjWnAsfpksWKaOd8AAAGOFjrv9wAABAMASDBGAiEA+8OvQzpgRf31\n" +
                "uLBsCE8ktCUfvsiRT7zWSqeXliA09TUCIQDcB7Xn97aEDMBKXIbdm5KZ9GjvRyoF\n" +
                "9skD5/4GneoMWzAlBgNVHREEHjAcggpnaXRodWIuY29tgg53d3cuZ2l0aHViLmNv\n" +
                "bTAKBggqhkjOPQQDAgNIADBFAiEAru2McPr0eNwcWNuDEY0a/rGzXRfRrm+6XfZe\n" +
                "SzhYZewCIBq4TUEBCgapv7xvAtRKdVdi/b4m36Uyej1ggyJsiesA\n" +
                "-----END CERTIFICATE-----\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(sslCert.getBytes(StandardCharsets.UTF_8));
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate)certificateFactory.generateCertificate(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        String alias = cert.getSubjectX500Principal().getName();
        trustStore.setCertificateEntry(alias, cert);


        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            return buildSslContext().getSocketFactory();
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException |
                 KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
