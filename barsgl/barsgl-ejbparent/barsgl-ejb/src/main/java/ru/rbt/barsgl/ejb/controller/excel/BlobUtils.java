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

    public boolean writeBlob(Connection connection,
                             final String table,
                             final String field,
                             final String pk,
                             final long id,
//                             final String fpath,
                             File file ) throws FileNotFoundException, SQLException {
        boolean result = false;
//        String sql = String.format(INSERT_blob, table, pk, id);
//        boolean result = execSQL (connection, sql);
//        if (result) {
//        File file = new File(fpath);
            PreparedStatement ps = null;
//            try {
                String sql = String.format(UPDATE_blob, table, field, pk);
                FileInputStream is = new FileInputStream(file);
                ps = connection.prepareStatement(sql);
                ps.setBinaryStream(1, is, (int)file.length());
                ps.setLong(2, id);
                ps.executeUpdate();

//                connection.commit();
//                ps.close();
//            } catch (FileNotFoundException e) {
//                result = false;
//                e.printStackTrace();
//            } catch (SQLException e) {
//                result = false;
//                e.printStackTrace();
//            }
//        }
        return result;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
    }}
