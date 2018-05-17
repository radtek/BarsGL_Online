package ru.rbt.ejbcore.controller.etc;

import javax.ejb.Local;
import java.io.IOException;

/**
 * Created by er22317 on 16.05.2018.
 */
@Local
public interface ITextResourceController {
    public String getContent(String resourceName) throws IOException;
}
