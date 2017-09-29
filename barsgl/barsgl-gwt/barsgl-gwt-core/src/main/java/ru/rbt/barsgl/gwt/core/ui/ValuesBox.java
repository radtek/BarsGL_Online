package ru.rbt.barsgl.gwt.core.ui;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class ValuesBox extends Composite implements IBoxValue<Serializable> {
	private final String STYLE_NORMAL = "list_scale_normal";
	private final String STYLE_SELECTED = "list_scale_selected";

	protected ListBox list;
	private Map<Serializable, String> map;
	private Map<Serializable, Integer> mapIndex;

	public ValuesBox(){
		this(new LinkedHashMap<Serializable, String>());
	}

	public ValuesBox(Map<Serializable, String> map){
		this.map = map;
		this.mapIndex = new LinkedHashMap<Serializable, Integer>();
		initWidget(configure());

		addFocusBlurHandler();
	}

	private Widget configure(){
		list = new ListBox(); 
		list.setHeight("24px");
		list.setStyleName(STYLE_NORMAL);

		int idx = 0;
		for (Entry<Serializable, String> entry : map.entrySet()) {
			list.addItem(entry.getValue());
			mapIndex.put(entry.getKey(), idx++);
		}
		return list;
	}

	private void addFocusBlurHandler() {
		list.addBlurHandler(new BlurHandler() {
			public void onBlur(BlurEvent event) {
				list.setStyleName(STYLE_NORMAL);               
			}
		});
		list.addFocusHandler(new FocusHandler() {
			public void onFocus(FocusEvent event) {
				list.setStyleName(STYLE_SELECTED);
			}
		});
	}

	@Override

	public void setValue(Serializable value) {			
		if (value instanceof String)
			value = ((String) value).trim(); 
		if (!validate() || !mapIndex.containsKey(value)) return;

		int idx = mapIndex.get(value);
		if (idx != -1) list.setSelectedIndex(idx);
	}

	@Override
	public Serializable getValue() {
		if (!validate()) return null;

		int idx = list.getSelectedIndex();
		if (idx == -1) return null;

		return getKeyByIndex(idx);
	}

	@Override
	public boolean hasValue() {	
		if  (!validate()) return false;

		return (list.getSelectedIndex() != -1);
	}

	@Override
	public boolean validate() {		
		return !mapIndex.isEmpty();
	}

	@Override
	public void setWidth(String width){
		// TODO надо сделать нормально!
		if ((width.length() > 2) && (width.substring(width.length()-2).equals("px"))) {
			int w = Integer.parseInt(width.substring(0, width.length()-2)) + 7;
			String wid = Integer.toString(w) + "px";
			list.setWidth(wid);
		} else {
			list.setWidth(width);
		}
	}


	@Override
	public void setEnabled(boolean enabled) {
		list.setEnabled(enabled);
		list.getElement().getStyle().setBackgroundColor(enabled ? "white" : "#f3f1e8" );
	}

	public boolean isEnabled(){
		return list.isEnabled();
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		setEnabled(!readOnly);
	}

	@Override
	public String toString(){
		return (validate()) ? list.getItemText(list.getSelectedIndex()): "";	
	}

	public int getItemCount() {
		return list.getItemCount();
	}
	
	public void addItem(Serializable key, String value) {
		map.put(key, value.trim()); 
		mapIndex.put(key, list.getItemCount());
		list.addItem(value.trim());
	}
		
	public void clear() {
		map.clear();
		mapIndex.clear();
		list.clear();
	}

	protected Serializable getKeyByIndex(int index){
		for (Entry<Serializable, Integer> entry: mapIndex.entrySet()){
			if (entry.getValue() == index){
				return entry.getKey();			
			}
		}
		return null;
	}

	private Serializable getKeyByValue(String text){
		for (Entry<Serializable, String> entry: map.entrySet()){
			if (entry.getValue().trim().equalsIgnoreCase(text)){
				return entry.getKey();
			}
		}
		return null;
	}

	public int getListIndex(String text){
		for (int i = 0; i < list.getItemCount(); i++){
			if (list.getItemText(i).equalsIgnoreCase(text)){
				return i;
			}
		}
		return -1;
	}

	public String getText() {
		if (!validate()) return null;

		int idx = list.getSelectedIndex();
		return idx >= 0 ? list.getItemText(idx) : "";
	}

	public void setText(String value) {
		if (!validate() || !map.containsValue(value)) return;
		int idx = getListIndex(value);
		if (idx != -1) list.setSelectedIndex(idx);
	}

	public void addChangeHandler(ChangeHandler handler) {
		list.addChangeHandler(handler);
	}

	public void setName(String name) {
		list.setName(name);
	}
	
	// count = 1 -> combobox
	// count > 1 -> listbox 
	public void setVisibleItemCount(int count){			
		list.setVisibleItemCount(count < 1 ? 1 : count);
	}

	public int getSelectedIndex(){
		return list.getSelectedIndex();
	}

	private void removeItemByIndex(int idx){
		Serializable key = getKeyByIndex(idx);

		map.remove(key);
		mapIndex.remove(key);

		list.setSelectedIndex(nextIndex(idx));
		list.removeItem(idx);

		reindex();
	}

	public void removeItem(){		
		int idx = list.getSelectedIndex();
		if (idx == -1) return;

		removeItemByIndex(idx);
	}

	public void removeItem(String text) {
		int idx = getListIndex(text);
		if(idx == -1) return;

		removeItemByIndex(idx);
	}

	private void reindex(){
		int idx = 0;
		for (Entry<Serializable, Integer> entry : mapIndex.entrySet()) {			
			 entry.setValue(idx++);
		}
	}

	private int nextIndex(int idx){
		return (idx == 0 && list.getItemCount() > 1) ? idx + 1 : idx - 1;
	}

	public void setSelectedIndex(int idx){
		if (list.getItemCount() > 0 && idx > -1 && idx < list.getItemCount()){
			list.setSelectedIndex(idx);
		}
	}

	public ArrayList<Serializable> getValues(){
		ArrayList<Serializable> list = new ArrayList<Serializable>();
		Set<Entry<Serializable, Integer>> entrySet =  mapIndex.entrySet();
		if (entrySet.isEmpty()) return list;

		for(Entry<Serializable, Integer> entry: entrySet){
			list.add(entry.getKey());
		}
		return list;
	}
}
