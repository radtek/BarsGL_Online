/*
 * ООО "Артком Системы" & "3G Banking Technologies" 2017
 */
package ru.rbt.barsgl.gwt.core.comp;

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.Date;
import ru.rbt.barsgl.gwt.core.ui.AreaBox;
import ru.rbt.barsgl.gwt.core.ui.DatePickerBox;
import ru.rbt.barsgl.gwt.core.ui.DecBox;
import ru.rbt.barsgl.gwt.core.ui.IntBox;
import ru.rbt.barsgl.gwt.core.ui.TxtBox;
import ru.rbt.barsgl.shared.ClientDateUtils;

/**
 *
 * @author Andrew Samsonov
 */
public class Components {

  /**
   * Создает поле ввода десятичного числа с двумя знаками после запятой
   * @return
   */
  public static DecBox createDecBox(int scale, int length, String width) {
    DecBox box = new DecBox(null);
    box.setScale(scale);
    box.setMaxLength(length);
    box.setVisibleLength(length);
    box.setWidth(width);
    return box;
  }

  /**
   * создает виджет, вложенный в горизонтальную панель заданной ширины
   * @param widget
   * @param width
   * @return
   */
  public static Widget createAlignWidget(Widget widget, String width) {
    HorizontalPanel panel = new HorizontalPanel();
    panel.add(widget);
    panel.setWidth(width);
    return panel;
  }

  /**
   * создает метку
   * @param text
   * @return
   */
  public static Label createLabel(String text) {
    return new Label(text);
  }

  /**
   * создает метку заданной ширины
   * @param text
   * @param width
   * @return
   */
  public static Label createLabel(String text, String width) {
    Label lab = new Label(text);
    lab.setWidth(width);
    return lab;
  }

  /**
   * Создаaeт текстовое поле для ввода целого числа заданной длины
   * @param length
   * @return
   */
  public static TxtBox createTxtIntBox(int length) {
    TxtBox res = createTxtBox(length);
    res.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        char charCode = event.getCharCode();
        if (!Character.isDigit(charCode)) {
          ((TextBox) event.getSource()).cancelKey();
        }
      }
    });
    return res;
  }

  /**
   * Создаaeт текстовое поле для ввода целого числа заданной длины и ширины
   * @param length
   * @return
   */
  public static TxtBox createTxtIntBox(int length, String width) {
    TxtBox box = createTxtIntBox(length);
    box.setWidth(width);
    return box;
  }

  /**
   * Создает поле ввода целого числа
   * @return
   */
  public static IntBox createIntBox() {
    return new IntBox();
  }

  /**
   * Создает поле ввода целого числа заданной длины
   * @return
   */
  public static IntBox createIntBox(int length) {
    IntBox intBox = createIntBox();
    intBox.setMaxLength(length);
    return intBox;
  }

  /**
   * Создает текстовое поле заданной длины
   * @param length
   * @return
   */
  public static TxtBox createTxtBox(int length) {
    TxtBox txtBox = new TxtBox();
    txtBox.setMaxLength(length);
    txtBox.setVisibleLength(length);
    return txtBox;
  }

  /**
   * Создает текстовое поле заданной длины и ширины
   * @param length
   * @return
   */
  public static TxtBox createTxtBox(int length, String width) {
    TxtBox txtBox = createTxtBox(length);
    txtBox.setWidth(width);
    return txtBox;
  }

  /**
   * Создает поле выбора даты
   * @return
   */
  public static DatePickerBox createDateBox() {
    return new DatePickerBox(new Date(), ClientDateUtils.TZ_CLIENT);
  }

  public static DatePickerBox createDateBox(Date date) {
    return new DatePickerBox(date, ClientDateUtils.TZ_CLIENT);
  }

  public static AreaBox createAreaBox() {
    AreaBox box = new AreaBox();
    return box;
  }

  public static AreaBox createAreaBox(String width, String height) {
    AreaBox box = new AreaBox();
    box.setSize(width, height);
    return box;
  }

  public static AreaBox createAreaBox(String width, String height, int length) {
    AreaBox box = createAreaBox(width, height);
    ((InputElement) box.getElement().cast()).setMaxLength(length);
    return box;
  }
  
}
