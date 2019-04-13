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
    private JButton btnPullFromServer;
    private JScrollPane tableHolder;
    private JButton handshakeWithUserButton;
    private JPanel rightPanel;
    private JLabel currentUser;
    private JScrollPane chatHistoryHolder;
    private JScrollPane channelsPane;
    private JComboBox userDropdown;
    private JButton btnInviteUser;
    private JButton btnRemoveUser;
    private JButton createChanelButton;
    private JScrollPane channelChatMessagesPane;

    private long selected_facebook_id = -1;

    public ChatDialog() {
        setContentPane(contentPane);
        setModal(true);

        //userModel = (DefaultTableModel) tableUsers.getModel();
        //tableUsers.setModel(userModel);
        ClientSingleton.inst().db.dispatcher.addListener(this);

        btnPullFromServer.addActionListener(e -> {
            try {
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
                var chat_message = messageInput.getText();

                var json = new JSONObject();
                json.put("message_type", "chat_message_to_channel");
                var jsonContent = new JSONObject();
                jsonContent.put("chat_message", chat_message);
                jsonContent.put("channel_uuid", selectedChannel.uuid);
                ClientSingleton.inst().sendMessageToChannel(selectedChannel, jsonContent);

                messageInput.setText("");

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        createChanelButton.addActionListener(e -> {
            try {
                ClientSingleton.inst().createNewChannel();
                ClientSingleton.inst().PullServerEvents();

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
        jTable.getColumnModel().getColumn(0).setMaxWidth(16);
        // Todo: Make cells non-editable: j.isCellEditable()
        jTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selected_facebook_id = Long.parseLong((String) jTable.getValueAt(jTable.getSelectedRow(), 1));

                System.out.println("selected_facebook_id: " + selected_facebook_id);
                localModelChanged();
            }
        });
        tableHolder.setViewportView(jTable);


        try { // TODO:Remove try
            var listModel = new DefaultListModel<Channel>();
            var channels = ClientSingleton.inst().db.getBuildedChannels();
            listModel.addAll(channels);

            var jlist = new JList<>(listModel);
            channelsPane.add(new JScrollPane(jlist));
            jlist.setCellRenderer(new ChannelRenderer());
        } catch (Exception ex) {
        }

        if (selectedChannel != null) {
            var options = getUserOptions();
            options.forEach(opt -> userDropdown.addItem(opt));
        }

/*
        userModel.setNumRows(2);
        for (var u : users) {
            Object[] objects = new Object[]{u.facebook_id, u.facebook_name};
            userModel.addRow(objects);
        }*/

        localModelChanged();
    }

    Channel selectedChannel = null;

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
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 5, new Insets(10, 10, 10, 10), -1, -1));
        btnPullFromServer = new JButton();
        btnPullFromServer.setText("Pull From Server");
        contentPane.add(btnPullFromServer, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tableHolder = new JScrollPane();
        contentPane.add(tableHolder, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 3, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 100), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(rightPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 4, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        chatHistoryHolder = new JScrollPane();
        rightPanel.add(chatHistoryHolder, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Current user: ");
        contentPane.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 16), null, 0, false));
        channelsPane = new JScrollPane();
        contentPane.add(channelsPane, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("messages for event sourcing");
        contentPane.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentUser = new JLabel();
        currentUser.setText("Label");
        contentPane.add(currentUser, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(211, 16), null, 0, false));
        handshakeWithUserButton = new JButton();
        handshakeWithUserButton.setText("HandshakeWithUser");
        contentPane.add(handshakeWithUserButton, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("channels");
        contentPane.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createChanelButton = new JButton();
        createChanelButton.setText("Create Chanel");
        contentPane.add(createChanelButton, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelChatMessagesPane = new JScrollPane();
        contentPane.add(channelChatMessagesPane, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(3, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        userDropdown = new JComboBox();
        panel1.add(userDropdown, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnInviteUser = new JButton();
        btnInviteUser.setText("Invite");
        panel1.add(btnInviteUser, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRemoveUser = new JButton();
        btnRemoveUser.setText("Remove");
        panel1.add(btnRemoveUser, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setEnabled(true);
        contentPane.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(4, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setEnabled(true);
        sendButton.setText("Send");
        panel2.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageInput = new JTextField();
        panel2.add(messageInput, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(148, 30), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    class UserOption {
        public long facebook_id;
        public String name;
        public MemberStatus memberStatus = MemberStatus.NOT_SET;

        @Override
        public String toString() {
            var ret = name;
            if (memberStatus != MemberStatus.NOT_SET)
                ret += " " + memberStatus;
            return ret;
        }
    }

    public ArrayList<UserOption> getUserOptions() {
        var lst = new ArrayList<UserOption>();
        var users = ClientSingleton.inst().db.getUsers();
        for (var user : users) {
            var opt = new UserOption();
            opt.facebook_id = user.facebook_id;
            opt.name = user.facebook_name;
            var mem = selectedChannel.getMember(user.facebook_id);
            if (mem != null)
                opt.memberStatus = mem.status;
            lst.add(opt);
        }
        return lst;
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


        var messages = ClientSingleton.inst().db.getAllCachedMessages();
        String[][] us = messages.stream().map(cached_message_row::toStringList).toArray(String[][]::new);

        var jTable = new JTable(us, cached_message_row.columnNames);
        jTable.getColumnModel().getColumn(0).setMaxWidth(16);
        jTable.getColumnModel().getColumn(1).setMaxWidth(125);
        jTable.getColumnModel().getColumn(1).setPreferredWidth(125);
        chatHistoryHolder.setViewportView(jTable);
    }

}
