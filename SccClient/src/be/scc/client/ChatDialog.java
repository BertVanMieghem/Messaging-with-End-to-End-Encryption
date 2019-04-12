package be.scc.client;

import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class ChatDialog extends JDialog implements SccListener {
    private JPanel contentPane;
    private JButton sendButton;
    private JTextField messageInput;
    private JButton btnPullPki;
    private JScrollPane tableHolder;
    private JButton handshakeWithUserButton;
    private JPanel rightPanel;
    private JLabel currentUser;
    private JScrollPane chatHistoryHolder;

    private long selected_facebook_id = -1;

    public ChatDialog() {
        setContentPane(contentPane);
        setModal(true);

        //userModel = (DefaultTableModel) tableUsers.getModel();
        //tableUsers.setModel(userModel);
        ClientSingleton.inst().db.dispatcher.addListener(this);

        btnPullPki.addActionListener(e -> {
            try {
                ClientSingleton.inst().PullUsers();
                ClientSingleton.inst().PullServerEvents();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        handshakeWithUserButton.addActionListener(e -> {
            try {
                ClientSingleton.inst().handshakeWithFacebookId(selected_facebook_id);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        sendButton.addActionListener(e -> {
            try {
                var text = messageInput.getText();

                var json = new JSONObject();
                json.put("message_type", "chat_message"); // Premature abstraction
                json.put("content", text);
                ClientSingleton.inst().sendMessageToFacebookId(selected_facebook_id, json.toString());

                messageInput.setText("");

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        messageInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void update() {
                localModelChanged();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("ChatDialog.windowClosing");
                System.exit(0);
            }
        });
        localModelChanged();
    }

    @Override
    public void SccModelChanged() {

        var users = ClientSingleton.inst().db.getUsers();
        String[][] us = users.stream().map(local_user::toStringList).toArray(String[][]::new);

        currentUser.setText("" + ClientSingleton.inst().db.facebook_id);

        // Inspired on: https://www.geeksforgeeks.org/java-swing-jtable/
        var jTable = new JTable(us, local_user.columnNames);
        // Todo: Make cells non-editable: j.isCellEditable()
        jTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selected_facebook_id = Long.parseLong((String) jTable.getValueAt(jTable.getSelectedRow(), 1));

                System.out.println("selected_facebook_id: " + selected_facebook_id);
                localModelChanged();
            }
        });
        tableHolder.setViewportView(jTable);
/*
        userModel.setNumRows(2);
        for (var u : users) {
            Object[] objects = new Object[]{u.facebook_id, u.facebook_name};
            userModel.addRow(objects);
        }*/

        localModelChanged();
    }

    private void localModelChanged() {
        rightPanel.setEnabled(this.selected_facebook_id != -1); // swing doesn't allow to disable a panel!
        var text = messageInput.getText();
        if (text != null && !text.equals("") && this.selected_facebook_id != -1) {
            sendButton.setEnabled(true);
            sendButton.setToolTipText("");
        } else if (this.selected_facebook_id != -1) {
            sendButton.setEnabled(false);
            sendButton.setToolTipText("Select an user first!");
        } else {
            sendButton.setEnabled(false);
            sendButton.setToolTipText("First type some text!");
        }


        var messages = ClientSingleton.inst().db.getMessagesForFacebookId(selected_facebook_id);
        String[][] us = messages.stream().map(cached_message_row::toStringList).toArray(String[][]::new);

        var jTable = new JTable(us, cached_message_row.columnNames);
        // Todo: Make cells non-editable: j.isCellEditable()
        /*jTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                //selected_facebook_id = Long.parseLong((String) jTable.getValueAt(jTable.getSelectedRow(), 1));

                System.out.println("selected_facebook_id: " + selected_facebook_id);
                localModelChanged();
            }
        });*/
        chatHistoryHolder.setViewportView(jTable);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 3, new Insets(10, 10, 10, 10), -1, -1));
        btnPullPki = new JButton();
        btnPullPki.setText("Pull Pki");
        contentPane.add(btnPullPki, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tableHolder = new JScrollPane();
        contentPane.add(tableHolder, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 100), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(rightPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setEnabled(true);
        rightPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setEnabled(true);
        sendButton.setText("Send");
        panel1.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageInput = new JTextField();
        panel1.add(messageInput, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(148, 30), null, 0, false));
        handshakeWithUserButton = new JButton();
        handshakeWithUserButton.setText("HandshakeWithUser");
        rightPanel.add(handshakeWithUserButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chatHistoryHolder = new JScrollPane();
        rightPanel.add(chatHistoryHolder, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Current user: ");
        contentPane.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 16), null, 0, false));
        currentUser = new JLabel();
        currentUser.setText("Label");
        contentPane.add(currentUser, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(211, 16), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
