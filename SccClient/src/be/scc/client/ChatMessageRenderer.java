package be.scc.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Objects;

public class ChatMessageRenderer extends JPanel implements ListCellRenderer<ChatMessage> {

    JLabel sanderLbl = new JLabel();
    JLabel messageLbl = new JLabel() {
        @Override
        protected void paintComponent(Graphics g) {
            Dimension arcs = new Dimension(20, 20);
            int width = getWidth();
            int height = getHeight();
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setColor(getBackground());
            graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);//paint background
            graphics.setColor(getForeground());
            //graphics.drawRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);//paint border
            super.paintComponent(g);

        }
    };

    Component glueLeft = Box.createHorizontalGlue();
    Component glueRight = Box.createHorizontalGlue();

    public ChatMessageRenderer() {
        this.add(sanderLbl);

        var bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        this.add(bottom);

        bottom.add(glueLeft);
        bottom.add(messageLbl);
        bottom.add(glueRight);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatMessage> list, ChatMessage listItem, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        try {
            this.setBorder(new EmptyBorder(5, 10, 5, 10));
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setOpaque(false);

            var user = ClientSingleton.inst().db.getUserWithFacebookId(listItem.from_facebook_id);

            sanderLbl.setVisible(true);
            if (index > 0) {
                var prevMessage = list.getModel().getElementAt(index - 1);
                var prevUser = ClientSingleton.inst().db.getUserWithFacebookId(prevMessage.from_facebook_id);
                if (Objects.equals(prevUser, user))
                    sanderLbl.setVisible(false);
            }

            sanderLbl.setText(user.facebook_name);
            sanderLbl.setForeground(Color.lightGray);
            sanderLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            messageLbl.setText(listItem.message);
            messageLbl.setToolTipText(listItem.date.toString());
            messageLbl.setBorder(new EmptyBorder(5, 8, 5, 8));


            if (Objects.equals(listItem.from_facebook_id, ClientSingleton.inst().db.facebook_id)) {
                glueLeft.setVisible(true);
                glueRight.setVisible(false);
                messageLbl.setBackground(new Color(0, 153, 255));
                messageLbl.setForeground(Color.white);
            } else {
                glueLeft.setVisible(false);
                glueRight.setVisible(true);
                messageLbl.setBackground(new Color(240, 240, 240));
                messageLbl.setForeground(Color.black);
            }

        } catch (SQLException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return this;
    }
}
