package Controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import model.ProductModel;
import model.game;

@WebServlet("/AddGame")
@MultipartConfig()
public class UploadGame extends HttpServlet {
    private static final long serialVersionUID = 1L;
    static String SAVE_DIR = "img";
    static ProductModel GameModels = new ProductModelDM();
    
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime now = LocalDateTime.now();
    
    // Set of allowed file extensions
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>();
    
    static {
        ALLOWED_EXTENSIONS.add("jpg");
        ALLOWED_EXTENSIONS.add("jpeg");
        ALLOWED_EXTENSIONS.add("png");
        ALLOWED_EXTENSIONS.add("gif");
    }
    
    public UploadGame() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/plain");
        out.write("Error: GET method is used but POST method is required");
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Collection<?> games = (Collection<?>) request.getSession().getAttribute("games");
        String savePath = request.getServletContext().getRealPath("") + File.separator + SAVE_DIR;
        game g1 = new game();

        String fileName = null;
        String message = "upload =\n";
        if (request.getParts() != null && request.getParts().size() > 0) {
            for (Part part : request.getParts()) {
                fileName = extractFileName(part);

                if (fileName != null && !fileName.equals("")) {
                    String extension = getFileExtension(fileName);
                    if (isAllowedExtension(extension) && isValidImage(part)) {
                        String sanitizedFileName = sanitizeFileName(fileName);
                        part.write(savePath + File.separator + sanitizedFileName);
                        g1.setImg(sanitizedFileName);
                        message = message + sanitizedFileName + "\n";
                    } else {
                        request.setAttribute("error", "Errore: Tipo di file non consentito o file non valido.");
                        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/errorPage.jsp");
                        dispatcher.forward(request, response);
                        return;
                    }
                } else {
                    request.setAttribute("error", "Errore: Bisogna selezionare almeno un file");
                }
            }
        }

        g1.setName(request.getParameter("nomeGame"));
        g1.setYears(request.getParameter("years"));
        g1.setAdded(dtf.format(now));
        g1.setQuantity(Integer.valueOf(request.getParameter("quantita")));
        g1.setPEG(Integer.valueOf(request.getParameter("PEG")));
        g1.setIva(Integer.valueOf(request.getParameter("iva")));
        g1.setGenere(request.getParameter("genere"));
        g1.setDesc(request.getParameter("desc"));
        g1.setPrice(Float.valueOf(request.getParameter("price")));

        try {
            GameModels.doSave(g1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        request.setAttribute("stato", "success!");
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/gameList?page=admin&sort=added DESC");
        dispatcher.forward(request, response);
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    private boolean isAllowedExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private boolean isValidImage(Part part) {
        try (InputStream input = part.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }
}
