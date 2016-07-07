/**
 * 
 */
package spta.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * @author araiyoshiyuki
 *
 */
public class ColorTableRenderer extends DefaultTableCellRenderer implements TableCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Component getTableCellRendererComponent(JTable tb,
			Object val, boolean isSelected,
			boolean hasFocus, int r, int c){
		
		Component returnMe = super.getTableCellRendererComponent(tb, val, isSelected, hasFocus, r, c);
		
		if (val instanceof Color) {
			Color color = (Color)val;
			returnMe.setBackground(color);
			if (returnMe instanceof JLabel) {
				JLabel jl = (JLabel)returnMe;
				jl.setOpaque(true);
				jl.setText(" ");
			}
		}
		return returnMe;		
	}

}
