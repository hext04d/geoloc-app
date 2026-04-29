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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LocationExporter {

    public enum Format { KMZ, KML, TXT }

    public static void export(Context context, List<SavedLocation> locations, Format format) {
        if (locations == null || locations.isEmpty()) return;

        File docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportsDir = new File(docsDir, "geoloc");
        
        if (!exportsDir.exists() && !exportsDir.mkdirs()) {
            Toast.makeText(context, "Erro ao criar pasta em Documentos/geoloc", Toast.LENGTH_LONG).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "geoloc_" + timestamp;
        File file;

        try {
            switch (format) {
                case KMZ:
                    file = new File(exportsDir, fileName + ".kmz");
                    saveAsKMZ(file, locations);
                    break;
                case KML:
                    file = new File(exportsDir, fileName + ".kml");
                    saveAsKML(file, locations);
                    break;
                case TXT:
                    file = new File(exportsDir, fileName + ".txt");
                    saveAsTXT(file, locations);
                    break;
                default:
                    return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao gravar o arquivo.", Toast.LENGTH_LONG).show();
            return;
        }

        showPostExportDialog(context, file, format);
    }

    private static void saveAsKMZ(File file, List<SavedLocation> locations) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry("doc.kml");
            zos.putNextEntry(entry);
            zos.write(generateKMLContent(locations).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private static void saveAsKML(File file, List<SavedLocation> locations) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateKMLContent(locations));
        }
    }

    private static void saveAsTXT(File file, List<SavedLocation> locations) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            for (SavedLocation loc : locations) {
                String name = loc.getNickname().isEmpty() ? loc.getAddress() : loc.getNickname();
                writer.write(String.format(Locale.getDefault(), 
                    "Nome: %s\nEndereço: %s\nLat: %.6f, Lon: %.6f\nData: %s\n-------------------\n",
                    name, loc.getAddress(), loc.getLatitude(), loc.getLongitude(), loc.getTimestamp()));
            }
        }
    }

    private static String generateKMLContent(List<SavedLocation> locations) {
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

    private static void showPostExportDialog(Context context, File file, Format format) {
        String displayPath = "Documents/geoloc/" + file.getName();
        new AlertDialog.Builder(context)
                .setTitle("Exportação concluída")
                .setMessage("Arquivo " + format.name() + " salvo em: " + displayPath)
                .setPositiveButton("Compartilhar", (dialog, which) -> shareFile(context, file, format))
                .setNegativeButton("Abrir", (dialog, which) -> openFile(context, file, format))
                .setNeutralButton("Fechar", null)
                .show();
    }

    private static void shareFile(Context context, File file, Format format) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(getMimeType(format));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Compartilhar " + format.name() + " via..."));
    }

    private static void openFile(Context context, File file, Format format) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, getMimeType(format));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        Intent chooser = Intent.createChooser(intent, "Escolha um aplicativo para abrir o " + format.name());
        try {
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "Nenhum aplicativo compatível instalado.", Toast.LENGTH_SHORT).show();
        }
    }

    private static String getMimeType(Format format) {
        switch (format) {
            case KMZ: return "application/vnd.google-earth.kmz";
            case KML: return "application/vnd.google-earth.kml+xml";
            case TXT: return "text/plain";
            default: return "*/*";
        }
    }
}
