import com.google.gson.Gson;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
@Path("/book")
public class Library {
    private final String error = "Server error, contact administrators";
    private boolean checkParams(String isbn,String autore, String titolo){
        return (isbn == null || isbn.trim().length() == 0) || (titolo == null || titolo.trim().length() == 0) || (autore == null || autore.trim().length() == 0);
    }

    private boolean checkParams(String libro, String utente){
        return (libro == null || libro.trim().length() == 0) || (utente == null || utente.trim().length() == 0);
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(){
        final String QUERY = "SELECT * FROM Libri";
        final List<Book> books = new ArrayList<>();
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            ResultSet results =  pstmt.executeQuery();
            while (results.next()){
                Book book = new Book();
                book.setTitolo(results.getString("Titolo"));
                book.setAutore(results.getString("Autore"));
                book.setISBN(results.getString("ISBN"));
                books.add(book);

            }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson(books);
        return Response.status(200).entity(obj).build();
    }

    @PUT
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@FormParam("ISBN") String isbn,
                           @FormParam("Titolo")String titolo,
                           @FormParam("Autore") String autore){
        if(checkParams(isbn, titolo, autore)) {
            String obj = new Gson().toJson("Parameters must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "UPDATE Libri SET Titolo = ?, Autore = ? WHERE ISBN = ?";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0]);//, data[1], data[2]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,titolo);
            pstmt.setString(2,autore);
            pstmt.setString(3,isbn);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " modificato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@FormParam("ISBN") String isbn,
                           @FormParam("Titolo")String titolo,
                           @FormParam("Autore") String autore){
        if(checkParams(isbn, titolo, autore)) {
            String obj = new Gson().toJson("Parameters must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "INSERT INTO Libri(ISBN,Titolo,Autore) VALUES(?,?,?)";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            pstmt.setString(2,autore);
            pstmt.setString(3,titolo);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " aggiunto con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }

    @DELETE
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@FormParam("ISBN") String isbn){
        if(isbn == null || isbn.trim().length() == 0){
            String obj = new Gson().toJson("ISBN must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String QUERY = "DELETE FROM Libri WHERE ISBN = ?";
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY )
        ) {
            pstmt.setString(1,isbn);
            pstmt.execute();
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ISBN:" + isbn + " eliminato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }


    @POST
    @Path("/prenota")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response prenota(@FormParam("ID") String libro,
                           @FormParam("Utente")String utente){
        if(checkParams(libro, utente)) {
            String obj = new Gson().toJson("Parameters must be valid");
            return Response.serverError().entity(obj).build();
        }
        final String select = "SELECT Quantita FROM Libri WHERE ID=" + libro;
        final String QUERY = "INSERT INTO Prestiti(libro, utente, inizioPrestito, finePrestito) VALUES(?,?,?,?)";
        final String Quantita = "UPDATE Libri SET Quantita=? WHERE ID=" + libro;
        final String[] data = Database.getData();
        try(

                Connection conn = DriverManager.getConnection(data[0]);
                PreparedStatement pstmt = conn.prepareStatement( QUERY );
                PreparedStatement pstmt_qta = conn.prepareStatement( Quantita );                
                Statement st = conn.createStatement();
        ) {
            ResultSet rs = st.executeQuery(select);
            int qta=-1;
            if(rs.next())
                qta = rs.getInt(1);
            if(qta>0){
                qta--;
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String inizioPrestito = sdfDate.format(new Date());
                int mese = Integer.parseInt(inizioPrestito.substring(5,7));
                mese++;
                String finePrestito = inizioPrestito.substring(0,5) +  mese + inizioPrestito.substring(7);
                pstmt.setString(1,libro);
                pstmt.setString(2,utente);
                pstmt.setString(3,inizioPrestito);
                pstmt.setString(4,finePrestito);
                pstmt.execute();
                pstmt_qta.setInt(1, qta);;
                pstmt_qta.execute();
            }
            else{
                String libri = new Gson().toJson("Libro non più disponibile");
                return Response.ok(libri,MediaType.APPLICATION_JSON).build();
            }
        }catch (SQLException e){
            e.printStackTrace();
            String obj = new Gson().toJson(error);
            return Response.serverError().entity(obj).build();
        }
        String obj = new Gson().toJson("Libro con ID:" + libro + " prenotato con successo");
        return Response.ok(obj,MediaType.APPLICATION_JSON).build();
    }
}
