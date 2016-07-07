/**
 * 
 */
package spta.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * @author araiyoshiyuki
 *
 */
public class MyCellRenderer extends JLabel implements ListCellRenderer<Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/* (non-Javadoc)
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	
	MyCellRenderer() {
		setOpaque(true);
	}
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		setText(" ");
		if (isSelected){
			setForeground(Color.black);
			setBackground((Color)value);
		} else {
			setForeground(Color.white);
			setBackground((Color)value);
		}
		return this;
	}

}
