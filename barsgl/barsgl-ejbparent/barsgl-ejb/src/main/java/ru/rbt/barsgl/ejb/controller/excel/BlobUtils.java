package ru.rbt.barsgl.ejb.controller.excel;

import java.io.*;
import java.sql.*;

/**
 * Created by er18837 on 02.11.2018.
 */
public class BlobUtils {
    private String INSERT_blob = "insert into %s(%s) values(%d)"    ;
    private String UPDATE_blob = "update %s set %s = ? where %s = ?";
    private String SELECT_blob = "select %s from %s where %s = %d"  ;
    protected final int  BUFFER_length = 2 * 1024;

    private String INSERT_clob = "insert into %s(%s) values(%d)"    ;
    private String UPDATE_clob = "update %s set %s = ? where %s = ?";
    private String SELECT_clob = "select %s from %s where id = %d"  ;

    public boolean writeBlob(Connection connection,
                         final String table,
                         final String field,
                         final String pk,
                         final long id,
                         File file ) throws FileNotFoundException, SQLException {
        if (connection == null)
            return false;

        String sql = String.format(UPDATE_blob, table, field, pk);
        FileInputStream is = new FileInputStream(file);
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setBinaryStream(1, is, (int)file.length());
        ps.setLong(2, id);
        ps.executeUpdate();
        return true;
    }

    // TODO далее не проверено XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

    public boolean writeBlob(Connection connection,
                             final String table,
                             final String field,
                             final String pk,
                             final long id,
                             final String fpath) throws FileNotFoundException, SQLException {
        File file = new File(fpath);
        return writeBlob(connection, table, field, pk, id, file);
    }

    private Blob readBlobField(Connection connection,
                               final String table,
                               final String field,
                               final String pk,
                               final int id) throws SQLException
    {
        String sql = String.format(SELECT_blob, field, table, pk, id);
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        Blob blob = null;
        if (rs.next())
            blob = rs.getBlob(1);
        return blob;
    }

    private long writeFromBlob2Stream(Blob blob, OutputStream out)
            throws SQLException, IOException
    {
        InputStream is = blob.getBinaryStream();
        int length = -1;
        long size = 0;
        byte[] buf = new byte[BUFFER_length];
        while ((length = is.read(buf)) != -1) {
            out.write(buf, 0, length);
            size += length;
        }
        is.close();
        return size;
    }

    public long readBlobToFile(Connection connection,
                               final String table,
                               final String field,
                               final String pk,
                               final int id,
                               final String fpath)
            throws IOException, SQLException
    {
        long size = 0;
        OutputStream fwriter = new FileOutputStream(fpath);
        Blob blob =  readBlobField(connection, table, field, pk, id);
        size = writeFromBlob2Stream(blob, fwriter);
        fwriter.close();
        return size;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public boolean writeClob(Connection connection,
                             final String table,
                             final String field,
                             final String pk,
                             final int id,
                             final String fpath) throws SQLException, FileNotFoundException, UnsupportedEncodingException {
        if (connection == null)
            return false;

        File file = new File(fpath);
        FileInputStream   fis = new FileInputStream  (file);
        InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
        BufferedReader    br  = new BufferedReader   (isr);

        String sql = String.format(UPDATE_clob, table, field, pk);
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setCharacterStream(1, br, (int)file.length());
        ps.setInt(2, id);
        ps.executeUpdate();

        return true;
    }

    public long readClobToFile(Connection connection,
                               final String table,
                               final String field,
                               final int id,
                               final String fpath)
            throws IOException, SQLException
    {
        long size = 0;
        BufferedWriter fwriter=new BufferedWriter(new FileWriter(fpath));
        Clob clob = readClobField (connection, table, field, id);
        size = readFromClob2Stream(clob, fwriter);
        fwriter.close();
        return size;
    }

    private Clob readClobField(Connection connection,
                               final String table,
                               final String field,
                               final int id) throws SQLException
    {
        String sql = String.format(SELECT_clob, field, table, id);
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        Clob clob = null;
        if (rs.next())
            clob = rs.getClob(1);
        return clob;
    }

    private long readFromClob2Stream(Clob clob, Writer out)
            throws SQLException, IOException
    {
        BufferedReader breader = new BufferedReader(clob.getCharacterStream());
        int length = -1;
        long size = 0;
        char[] buf = new char[BUFFER_length];
        while ((length = breader.read(buf, 0, BUFFER_length)) != -1) {
            out.write(buf, 0, length);
            size += length;
        }
        breader.close();
        return size;
    }

    public String readClobData(Connection connection,
                               final String table,
                               final String field,
                               final int id)
    {
        StringBuffer buffer = new StringBuffer();
        try {
            Clob clob = readClobField (connection, table, field, id);
            BufferedReader reader;
            reader = new BufferedReader(clob.getCharacterStream());
            char[] buf = new char[BUFFER_length];
            int length = -1;
            try {
                while ((length=reader.read(buf,0,BUFFER_length)) != -1){
                    if (length == BUFFER_length)
                        buffer.append(String.valueOf(buf));
                    else {
                        String tmp = String.valueOf(buf)
                                .substring(0, length);
                        buffer.append(tmp);
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Функция выполнения SQL-запроса
     * @param sql текст запроса
     * @return результат выполнения запроса
     */
    public boolean execSQL (Connection connection, final String sql) throws SQLException {
        if (connection == null)
            return false;

        Statement statement = connection.createStatement();
        statement.execute(sql);
        statement.close();

        return true;
    }
}
