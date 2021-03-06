// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.Serializable;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

public final class ReorderbleList<E extends ListItem> extends JList<E> {
  private static final AlphaComposite ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f);
  private transient MouseInputListener rbl;
  private Color rubberBandColor;
  private final Path2D rubberBand = new Path2D.Double();

  public ReorderbleList(ListModel<E> model) {
    super(model);
  }

  @Override public void updateUI() {
    setSelectionForeground(null); // Nimbus
    setSelectionBackground(null); // Nimbus
    setCellRenderer(null);
    setTransferHandler(null);
    removeMouseListener(rbl);
    removeMouseMotionListener(rbl);
    super.updateUI();

    rubberBandColor = makeRubberBandColor(getSelectionBackground());
    setLayoutOrientation(JList.HORIZONTAL_WRAP);
    setVisibleRowCount(0);
    setFixedCellWidth(62);
    setFixedCellHeight(62);
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    setCellRenderer(new ListItemListCellRenderer<>());
    rbl = new RubberBandingListener();
    addMouseMotionListener(rbl);
    addMouseListener(rbl);

    // putClientProperty("List.isFileList", Boolean.TRUE);
    getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    setTransferHandler(new ListItemTransferHandler());
    setDropMode(DropMode.INSERT);
  }

  @Override protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (getDragEnabled()) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setPaint(getSelectionBackground());
    g2.draw(rubberBand);
    g2.setComposite(ALPHA);
    g2.setPaint(rubberBandColor);
    g2.fill(rubberBand);
    g2.dispose();
  }

  private static Color makeRubberBandColor(Color c) {
    int r = c.getRed();
    int g = c.getGreen();
    int b = c.getBlue();
    return r > g ? r > b ? new Color(r, 0, 0) : new Color(0, 0, b)
           : g > b ? new Color(0, g, 0) : new Color(0, 0, b);
  }

  public Path2D getRubberBand() {
    return rubberBand;
  }

  private class RubberBandingListener extends MouseInputAdapter {
    private final Point srcPoint = new Point();

    @Override public void mouseDragged(MouseEvent e) {
      JList<?> l = (JList<?>) e.getComponent();
      if (l.getDragEnabled()) {
        return;
      }
      Point destPoint = e.getPoint();
      Path2D rb = getRubberBand();
      rb.reset();
      rb.moveTo(srcPoint.x, srcPoint.y);
      rb.lineTo(destPoint.x, srcPoint.y);
      rb.lineTo(destPoint.x, destPoint.y);
      rb.lineTo(srcPoint.x, destPoint.y);
      rb.closePath();

      // JDK 1.7.0: l.setSelectedIndices(getIntersectsIcons(l, rubberBand));
      int[] indices = IntStream.range(0, l.getModel().getSize())
          .filter(i -> rb.intersects(l.getCellBounds(i, i))).toArray();
      l.setSelectedIndices(indices);
      l.repaint();
    }

    @Override public void mouseReleased(MouseEvent e) {
      JList<?> l = (JList<?>) e.getComponent();
      l.setFocusable(true);
      // if (Objects.isNull(srcPoint) || !getDragEnabled()) {
      //   Component glassPane = l.getRootPane().getGlassPane();
      //   glassPane.setVisible(false);
      // }
      getRubberBand().reset();
      l.setDragEnabled(l.getSelectedIndices().length > 0);
      l.repaint();
    }

    @Override public void mousePressed(MouseEvent e) {
      JList<?> l = (JList<?>) e.getComponent();
      int index = l.locationToIndex(e.getPoint());
      Rectangle rect = l.getCellBounds(index, index);
      if (rect.contains(e.getPoint())) {
        l.setFocusable(true);
        if (l.getDragEnabled()) {
          return;
        }
        // System.out.println("ccc:" + startSelectedIndex);
        l.setSelectedIndex(index);
      } else {
        l.clearSelection();
        l.getSelectionModel().setAnchorSelectionIndex(-1);
        l.getSelectionModel().setLeadSelectionIndex(-1);
        l.setFocusable(false);
        l.setDragEnabled(false);
      }
      srcPoint.setLocation(e.getPoint());
      l.repaint();
    }
    // // JDK 1.7.0
    // private static int[] getIntersectsIcons(JList<?> l, Shape rect) {
    //   ListModel model = l.getModel();
    //   List<Integer> ll = new ArrayList<>(model.getSize());
    //   for (int i = 0; i < model.getSize(); i++) {
    //     if (rect.intersects(l.getCellBounds(i, i))) {
    //       ll.add(i);
    //     }
    //   }
    //   // JDK 1.8.0: return ll.stream().mapToInt(Integer::intValue).toArray();
    //   int[] il = new int[ll.size()];
    //   for (int i = 0; i < ll.size(); i++) {
    //     il[i] = ll.get(i);
    //   }
    //   return il;
    // }
  }
}

class SelectedImageFilter extends RGBImageFilter {
  // public SelectedImageFilter() {
  //   canFilterIndexColorModel = false;
  // }

  @Override public int filterRGB(int x, int y, int argb) {
    // Color color = new Color(argb, true);
    // float[] array = new float[4];
    // color.getComponents(array);
    // return new Color(array[0], array[1], array[2] * .5f, array[3]).getRGB();
    return (argb & 0xFF_FF_FF_00) | ((argb & 0xFF) >> 1);
  }
}

// class DotBorder extends EmptyBorder {
//   protected DotBorder(Insets borderInsets) {
//     super(borderInsets);
//   }
//   protected DotBorder(int top, int left, int bottom, int right) {
//     super(top, left, bottom, right);
//   }
//   @Override public boolean isBorderOpaque() {
//     return true;
//   }
//   @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
//     Graphics2D g2 = (Graphics2D) g.create();
//     g2.translate(x, y);
//     g2.setPaint(new Color(~SystemColor.activeCaption.getRGB()));
//     // new Color(200, 150, 150));
//     // g2.setStroke(dashed);
//     // g2.drawRect(0, 0, w - 1, h - 1);
//     BasicGraphicsUtils.drawDashedRect(g2, 0, 0, w, h);
//     g2.dispose();
//   }
//   // @Override public Insets getBorderInsets(Component c)
//   // @Override public Insets getBorderInsets(Component c, Insets insets)
// }

class ListItemListCellRenderer<E extends ListItem> implements ListCellRenderer<E> {
  private final JPanel renderer = new JPanel(new BorderLayout());
  private final JLabel icon = new JLabel((Icon) null, SwingConstants.CENTER);
  private final JLabel label = new JLabel("", SwingConstants.CENTER);
  // private final Border dotBorder = new DotBorder(2, 2, 2, 2);
  // private final Border empBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
  private final Border focusBorder = UIManager.getBorder("List.focusCellHighlightBorder");
  private final Border noFocusBorder; // = UIManager.getBorder("List.noFocusBorder");

  protected ListItemListCellRenderer() {
    Border b = UIManager.getBorder("List.noFocusBorder");
    if (Objects.isNull(b)) { // Nimbus???
      Insets i = focusBorder.getBorderInsets(label);
      b = BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right);
    }
    noFocusBorder = b;
    icon.setOpaque(false);
    label.setForeground(renderer.getForeground());
    label.setBackground(renderer.getBackground());
    label.setBorder(noFocusBorder);
    renderer.setOpaque(false);
    renderer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    renderer.add(icon);
    renderer.add(label, BorderLayout.SOUTH);
  }

  @Override public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
    label.setText(value.title);
    // label.setBorder(cellHasFocus ? dotBorder : empBorder);
    label.setBorder(cellHasFocus ? focusBorder : noFocusBorder);
    if (isSelected) {
      icon.setIcon(value.sicon);
      label.setForeground(list.getSelectionForeground());
      label.setBackground(list.getSelectionBackground());
      label.setOpaque(true);
    } else {
      icon.setIcon(value.nicon);
      label.setForeground(list.getForeground());
      label.setBackground(list.getBackground());
      label.setOpaque(false);
    }
    return renderer;
  }
}

class ListItem implements Serializable {
  private static final long serialVersionUID = 1L;
  public final ImageIcon nicon;
  public final ImageIcon sicon;
  public final String title;

  protected ListItem(String title, String iconfile) {
    this.nicon = new ImageIcon(getClass().getResource(iconfile));
    ImageProducer ip = new FilteredImageSource(nicon.getImage().getSource(), new SelectedImageFilter());
    this.sicon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(ip));
    this.title = title;
  }
}
