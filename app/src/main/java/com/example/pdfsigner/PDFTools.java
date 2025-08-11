package com.example.pdfsigner;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class PDFTools {

    private PDDocument doc;
    private Uri uri;
    Context context;
    public PDFTools(Context context) {
        this.context = context;
        PDFBoxResourceLoader.init(context);
    }

    public void open(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            doc = PDDocument.load(inputStream);
            this.uri = uri;
            Log.d("signer","open pdf done");
        } catch (Exception ex) {
            Log.e("signer","open pdf exception");
            Log.e("signer",ex.toString());
        }
    }

    public void getPosition( int pagenumber) {
        PDPage page = doc.getPage(pagenumber - 1);
        Log.e("BOXPDF1", page.getMediaBox().getUpperRightX() + "/" +  page.getMediaBox().getUpperRightY());
    }

    public PDPage getPDPage( int pagenumber) {
        return doc.getPage(pagenumber - 1);
    }

    public void insertImage(File file, int xpos, int ypos, int width, int height, int pagenumber) {
        try {
            PDPage page = doc.getPage(pagenumber - 1);
            PDImageXObject pdImage = PDImageXObject.createFromFile(file, doc);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page,true,false);
            contentStream.drawImage(pdImage, xpos, ypos, width, height);
            contentStream.close();
            OutputStream outputStream  = context.getContentResolver().openOutputStream(uri);
            doc.save(outputStream);
            doc.close();
            Log.d("signer","insertImage done");
        } catch (Exception ex) {
            Log.e("signer","insertImage exception");
            Log.e("signer",ex.toString());
        }
    }

    public void insertText(String text, int pagenumber, int x, int y) {
        try {
            PDPage page = doc.getPage(pagenumber - 1);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page,true,false);
            contentStream.beginText();
            PDFont font = PDType1Font.TIMES_ROMAN;
            Log.e("BOXPDF", page.getMediaBox().getWidth() + "/" +  page.getMediaBox().getHeight());
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 20);
            contentStream.newLineAtOffset(x,y);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();
            OutputStream outputStream  = context.getContentResolver().openOutputStream(uri);
            doc.save(outputStream);
            doc.close();
            Log.d("signer","insertImage done");
        } catch (Exception ex) {
            Log.e("signer","insertImage exception");
            Log.e("signer",ex.toString());
        }
    }
}
