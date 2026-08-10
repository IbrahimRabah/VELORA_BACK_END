package com.velora.api.export.service;

import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Renders HTML into a PDF.
 *
 * <p>HTML rather than a drawing API on purpose. VELORA's documents are Arabic and
 * right-to-left, and laying that out by positioning text runs means implementing
 * character shaping and bidirectional ordering by hand. A browser engine already
 * solves both, so the template stays ordinary HTML with {@code direction: rtl} and
 * the layout can change without touching Java.
 *
 * <p>Two things are non-negotiable for Arabic output:
 * <ul>
 *   <li>An <b>embedded Arabic font</b>. A PDF has no system fonts to fall back on;
 *       without one every Arabic character renders as an empty box, silently.</li>
 *   <li>The <b>RTL support module</b>, which reorders bidirectional text. Without it
 *       Arabic renders reversed and Latin digits inside it land in the wrong
 *       place.</li>
 * </ul>
 *
 * <p>Note the package: {@code io.github.openhtmltopdf}. The original
 * {@code com.openhtmltopdf} was archived; this is the maintained community fork.
 */
@Service
public class PdfRenderer {

    private static final Logger log = LoggerFactory.getLogger(PdfRenderer.class);

    private static final String FONT_REGULAR = "fonts/Amiri-Regular.ttf";
    private static final String FONT_BOLD = "fonts/Amiri-Bold.ttf";
    private static final String FONT_FAMILY = "Amiri";

    private final File regularFont;
    private final File boldFont;

    public PdfRenderer() {
        // The renderer's own logging is extremely chatty at INFO.
        XRLog.setLevel(XRLog.CSS_PARSE, Level.WARNING);
        XRLog.setLevel(XRLog.EXCEPTION, Level.WARNING);

        this.regularFont = extractFont(FONT_REGULAR, "velora-amiri-regular");
        this.boldFont = extractFont(FONT_BOLD, "velora-amiri-bold");
    }

    /**
     * @param html a complete HTML document
     * @return the rendered PDF bytes
     */
    public byte[] render(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);

            /*
             * Both faces register under the SAME family name. The renderer picks the
             * bold file when the CSS asks for weight 700, which is what makes
             * `font-weight: bold` in the template actually produce bold Arabic rather
             * than a synthetically smeared regular face.
             */
            if (regularFont != null) {
                builder.useFont(regularFont, FONT_FAMILY, 400,
                        BaseRendererBuilder.FontStyle.NORMAL, true);
            }
            if (boldFont != null) {
                builder.useFont(boldFont, FONT_FAMILY, 700,
                        BaseRendererBuilder.FontStyle.NORMAL, true);
            }

            // Bidirectional reordering. Arabic comes out backwards without this.
            builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
            builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
            builder.defaultTextDirection(BaseRendererBuilder.TextDirection.RTL);

            builder.toStream(out);
            builder.run();

            return out.toByteArray();

        } catch (IOException ex) {
            throw new IllegalStateException("Could not render the PDF", ex);
        }
    }

    /**
     * The renderer needs a real File, but the font ships inside the jar. Copied to a
     * temp file once at startup rather than on every render.
     */
    private File extractFont(String resourcePath, String prefix) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.error("Arabic font {} is missing. PDFs will render Arabic as empty "
                        + "boxes. Download Amiri and place it in resources/fonts/.",
                        resourcePath);
                return null;
            }

            Path temp = Files.createTempFile(prefix, ".ttf");
            temp.toFile().deleteOnExit();
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Loaded PDF font {}", resourcePath);
            return temp.toFile();

        } catch (IOException ex) {
            log.error("Could not load the PDF font {}", resourcePath, ex);
            return null;
        }
    }
}
