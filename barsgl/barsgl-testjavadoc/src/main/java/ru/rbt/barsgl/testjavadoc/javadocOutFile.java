package ru.rbt.barsgl.testjavadoc;

import java.io.File;

/**
 * Created by ER18837 on 06.07.15.
 */
public interface javadocOutFile {
    String[] columnNames = {"Класс / Метод", "Описание", "FSD"};

    abstract public boolean init(String filePath, String fileName, String template);
    abstract public void writeClass(String className, String comment);
    abstract public void writeMethod(String methodName, String ... params);
    abstract public void close();
}
