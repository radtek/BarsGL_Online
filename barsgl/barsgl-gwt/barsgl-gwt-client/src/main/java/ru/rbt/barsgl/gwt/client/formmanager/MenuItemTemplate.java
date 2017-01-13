package ru.rbt.barsgl.gwt.client.formmanager;

import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Created by akichigi on 07.04.15.
 */

public interface MenuItemTemplate extends SafeHtmlTemplates{
    @Template("<img src=\"{0}\" style =\"vertical-align: top; \"/><span style=\"vertical-align: top; \">{1}</span>")
    SafeHtml createItem(SafeUri uri, SafeHtml text);
}
