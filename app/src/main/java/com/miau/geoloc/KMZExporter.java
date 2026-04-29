package com.miau.geoloc;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KMZExporter {

    public static void exportToKMZ(Context context, List<SavedLocation> locations) {
        if (locations == null || locations.isEmpty()) return;

        String kmlContent = generateKML(locations);
        
        // Pasta geoloc dentro de Documentos
        File docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportsDir = new File(docsDir, "geoloc");
        
        if (!exportsDir.exists()) {
            if (!exportsDir.mkdirs()) {
                Toast.makeText(context, "Erro ao criar pasta em Documentos/geoloc", Toast.LENGTH_LONG).show();
                return;
            }
        }

        String fileName = "geoloc_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".kmz";
        File kmzFile = new File(exportsDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(kmzFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry entry = new ZipEntry("doc.kml");
            zos.putNextEntry(entry);
            zos.write(kmlContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao gravar o arquivo KMZ.", Toast.LENGTH_LONG).show();
            return;
        }

        showPostExportDialog(context, kmzFile);
    }

    private static void showPostExportDialog(Context context, File file) {
        String displayPath = "Documents/geoloc/" + file.getName();

        new AlertDialog.Builder(context)
                .setTitle("KMZ Exportado")
                .setMessage("Arquivo salvo em: " + displayPath)
                .setPositiveButton("Compartilhar", (dialog, which) -> shareFile(context, file))
                .setNegativeButton("Abrir", (dialog, which) -> openFile(context, file))
                .setNeutralButton("Fechar", null)
                .show();
    }

    private static String generateKML(List<SavedLocation> locations) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("<Document>\n");
        sb.append("  <name>Geoloc Saved Locations</name>\n");

        for (SavedLocation loc : locations) {
            sb.append("  <Placemark>\n");
            String name = loc.getNickname().isEmpty() ? loc.getAddress() : loc.getNickname();
            sb.append("    <name>").append(escapeXml(name)).append("</name>\n");
            sb.append("    <description>\n");
            sb.append("      Endereço: ").append(escapeXml(loc.getAddress())).append("\n");
            sb.append("      Data: ").append(escapeXml(loc.getTimestamp())).append("\n");
            sb.append("    </description>\n");
            sb.append("    <Point>\n");
            sb.append("      <coordinates>")
                    .append(loc.getLongitude()).append(",")
                    .append(loc.getLatitude()).append(",0")
                    .append("</coordinates>\n");
            sb.append("    </Point>\n");
            sb.append("  </Placemark>\n");
        }

        sb.append("</Document>\n");
        sb.append("</kml>");
        return sb.toString();
    }

    private static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    private static void shareFile(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/vnd.google-earth.kmz");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Compartilhar KMZ via..."));
    }

    private static void openFile(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.google-earth.kmz");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        Intent chooser = Intent.createChooser(intent, "Escolha um aplicativo para abrir o KMZ");
        try {
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "Nenhum aplicativo compatível instalado.", Toast.LENGTH_SHORT).show();
        }
    }
}
