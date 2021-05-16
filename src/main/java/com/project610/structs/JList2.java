package com.project610.structs;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Set;

public class JList2<T> extends JList<T> {

    public JList2() {
        super();
        clear();
    }

    public ArrayList<T> getItems() {
        ArrayList<T> items = new ArrayList<>();
        for (int i = 0; i < getModel().getSize(); i++) {
            items.add(getModel().getElementAt(i));
        }
        return items;
    }

    public void clear() {
        setModel(new DefaultListModel<>());
    }

    public JList2<T> add(T item) {
        DefaultListModel<T> listModel = (DefaultListModel<T>)getModel();
        listModel.addElement(item);
        setModel(listModel);
        return this;
    }

    public JList2<T> addAll(T... items) {
        DefaultListModel<T> listModel = (DefaultListModel<T>)getModel();
        for (T item : items) {
            listModel.addElement(item);
        }
        setModel(listModel);
        return this;
    }

    public JList2<T> addAll(Set<T> items) {
        DefaultListModel<T> listModel = (DefaultListModel<T>)getModel();
        for (T item : items) {
            listModel.addElement(item);
        }
        setModel(listModel);
        return this;
    }
}
