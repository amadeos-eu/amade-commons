package eu.amadeos.commons.print;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Printing a {@link Pageable} through {@code java.awt.print}: the page format for a paper size, the
 * system's print dialog, a PostScript file, and the names of the printers (CUPS on amadeOS).
 *
 * <p>The pages decide what is drawn; this class only decides where it goes.
 */
public final class PrintJobs {
    private static final String POSTSCRIPT = DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType();

    private PrintJobs() {
    }

    // --8<-- [start:page-format]
    /**
     * A page format for the paper.
     *
     * @param landscape true to turn the sheet; the paper stays in portrait and the printer turns the graphics
     * @param margin    the margin on every side, in points, outside which the pages do not draw; 0 when the
     *                  pages lay out their own margins
     */
    public static PageFormat pageFormat(PaperSize paper, boolean landscape, double margin) {
        Objects.requireNonNull(paper, "paper");
        double width = paper.width();
        double height = paper.height();
        if (margin < 0 || 2 * margin >= Math.min(width, height)) {
            throw new IllegalArgumentException("Margin does not fit the paper: " + margin);
        }
        Paper sheet = new Paper();
        sheet.setSize(width, height);
        sheet.setImageableArea(margin, margin, width - 2 * margin, height - 2 * margin);
        PageFormat format = new PageFormat();
        format.setPaper(sheet);
        format.setOrientation(landscape ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        return format;
    }
    // --8<-- [end:page-format]

    // --8<-- [start:attributes]
    /**
     * The paper and the job name for the printer. The orientation is deliberately not requested: the page
     * format already turns landscape pages, and asking the printer as well turns them twice on some drivers.
     */
    public static PrintRequestAttributeSet attributes(PaperSize paper, String jobName) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(paper.media());
        attributes.add(new JobName(jobName, null));
        return attributes;
    }
    // --8<-- [end:attributes]

    /**
     * Shows the system's print dialog for the pages and prints them. Swing components that paint the pages
     * must only be used on the event dispatch thread, so call it there when they do.
     *
     * @return false if the user cancelled the dialog
     */
    public static boolean printWithDialog(Pageable pages, PaperSize paper, String jobName) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(jobName);
        job.setPageable(pages);
        PrintRequestAttributeSet attributes = attributes(paper, jobName);
        if (!job.printDialog(attributes)) return false;
        job.print(attributes);
        return true;
    }

    // --8<-- [start:post-script]
    /**
     * Writes the pages to a PostScript file through the JDK's own PostScript stream printer, so it works
     * without any printer or print system installed.
     */
    public static void writePostScript(Pageable pages, Path file, String jobName) throws PrinterException, IOException {
        StreamPrintServiceFactory[] factories = StreamPrintServiceFactory.lookupStreamPrintServiceFactories(
                DocFlavor.SERVICE_FORMATTED.PAGEABLE, POSTSCRIPT);
        if (factories.length == 0) throw new PrinterException("This Java has no PostScript printer");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(factories[0].getPrintService(out));
            job.setJobName(jobName);
            job.setPageable(pages);
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(new JobName(jobName, null));
            job.print(attributes);
        }
    }
    // --8<-- [end:post-script]

    /** The printers the system knows, e.g. for a printer field; empty without a print system. */
    public static List<String> printerNames() {
        List<String> names = new ArrayList<>();
        try {
            for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
                names.add(service.getName());
            }
        } catch (RuntimeException e) {
            // no print system: an empty list
        }
        return names;
    }
}
