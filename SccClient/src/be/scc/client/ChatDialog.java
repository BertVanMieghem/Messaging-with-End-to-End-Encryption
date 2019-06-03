package be.scc.client;

import be.scc.common.Util;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class ChatDialog extends JDialog implements SccListener {
    private JPanel contentPane;
    private JButton sendButton;
    private JTextField messageInput;
    private JButton btnPullFromServer;
    private JScrollPane tableHolder;
    private JPanel rightPanel;
    private JLabel currentUser;
    private JScrollPane chatHistoryHolder;
    private JScrollPane channelsPane;
    private JComboBox userDropdown;
    private JButton btnInviteUser;
    private JButton btnRemoveUser;
    private JButton createChanelButton;
    private JScrollPane channelChatMessagesPane;
    private JScrollPane channelFileMessagesPane;
    private JButton btnAcceptInvite;
    private JTextField txtChannelName;
    private JButton renameChannelButton;
    private JCheckBox autoPullCheckBox;
    private JButton sendFileButton;
    private FileMessage selectedFile = null;
    private UUID selectedChannelUuid = null;
    private boolean isAutoPulling;


    private ActionListener autoPollingAction = evt -> {
        //System.out.println("autoPollingAction");
        try {
            ClientSingleton.inst().pullServerEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };
    private Timer autoPullCheckedTimer = new Timer(500, autoPollingAction);

    public ChatDialog() {
        setContentPane(contentPane);
        setModal(true);

        ClientSingleton.inst().db.dispatcher.addListener(this);

        btnPullFromServer.addActionListener(e -> {
            try {
                ClientSingleton.inst().pullServerEvents();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        sendButton.addActionListener(e -> {
            try {
                var chat_message = messageInput.getText();
                var ch = getSelectedChanel();

                var json = new JSONObject();
                json.put("message_type", "chat_message_to_channel");
                var jsonContent = new JSONObject();
                jsonContent.put("chat_message", chat_message);
                jsonContent.put("channel_uuid", ch.uuid);
                json.put("content", jsonContent);
                ClientSingleton.inst().sendMessageToChannelMembers(ch, json);

                messageInput.setText("");

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        sendFileButton.addActionListener(e -> {
            try {
                JFileChooser chooser = new JFileChooser();
                int choice = chooser.showOpenDialog(ChatDialog.this);
                if (choice != JFileChooser.APPROVE_OPTION) return;
                File chosenFile = chooser.getSelectedFile();
                String fileName = chosenFile.getName();
                String extension = "";

                int i = fileName.lastIndexOf('.');
                if (i > 0)
                    extension = fileName.substring(i);


                FileInputStream fileInputStream = new FileInputStream(chosenFile);
                byte[] dataByte = new byte[(int) chosenFile.length()];
                fileInputStream.read(dataByte);

                var ch = getSelectedChanel();

                var json = new JSONObject();
                json.put("message_type", "file_message_to_channel");
                var jsonContent = new JSONObject();
                jsonContent.put("file_message", Util.base64(dataByte));
                jsonContent.put("file_name", chosenFile.getName());
                jsonContent.put("file_extension", extension);
                jsonContent.put("channel_uuid", ch.uuid);
                json.put("content", jsonContent);

                ClientSingleton.inst().sendFileToChannelMembers(ch, json);
                System.out.println("Sent file '" + chosenFile.getName() + "' to channel");

            } catch (Exception el) {
                el.printStackTrace();
            }
        });

        createChanelButton.addActionListener(e -> {
            try {
                ClientSingleton.inst().createNewChannel();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        btnInviteUser.addActionListener(e -> {
            try {
                var userOption = getSelectedUserFromDropdown();
                ClientSingleton.inst().inviteUserToChannel(getSelectedChanel(), userOption.facebook_id);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        btnRemoveUser.addActionListener(e -> {
            try {
                var userOption = getSelectedUserFromDropdown();
                ClientSingleton.inst().removeUserToChannel(getSelectedChanel(), userOption.facebook_id);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        btnAcceptInvite.addActionListener(e -> {
            try {
                ClientSingleton.inst().acceptInvite(getSelectedChanel());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        renameChannelButton.addActionListener(e -> {
            try {
                var new_channel_name = txtChannelName.getText();
                ClientSingleton.inst().renameChannel(getSelectedChanel(), new_channel_name);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        userDropdown.addActionListener(e -> {
            localModelChanged();
        });
        isAutoPulling = !autoPullCheckBox.isSelected();
        autoPullCheckBox.addActionListener(e -> {
            localModelChanged();
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

    private UserOption getSelectedUserFromDropdown() {
        return (UserOption) userDropdown.getSelectedItem();
    }


    @Override
    public void sccModelChanged() {

        currentUser.setText("" + ClientSingleton.inst().db.facebook_id);
        setTitle("" + ClientSingleton.inst().db.facebook_id);

        {
            var users = ClientSingleton.inst().db.getUsers();
            String[][] us = users.stream().map(Local_user::toStringList).toArray(String[][]::new);

            // Inspired on: https://www.geeksforgeeks.org/java-swing-jtable/
            var jTable = new JTable(us, Local_user.columnNames);
            jTable.getColumnModel().getColumn(0).setMaxWidth(18);
            tableHolder.setViewportView(jTable);
        }

        {
            var listModel = new DefaultListModel<Channel>();
            var channels = ClientSingleton.inst().db.getBuildedChannels();
            listModel.addAll(channels);

            var jlist = new JList<>(listModel);
            jlist.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    selectedChannelUuid = jlist.getSelectedValue().uuid;

                    var options = getUserOptions();
                    userDropdown.removeAllItems();
                    // The following triggeres a change in the dropdown that we explicitly need to disable.
                    options.forEach(opt -> userDropdown.addItem(opt));

                    //System.out.println("selectedChannel: " + getSelectedChanel().name);
                    localModelChanged();
                }
            });

            channelsPane.setViewportView(new JScrollPane(jlist));
            jlist.setCellRenderer(new ChannelRenderer());
        }

        {
            var messages = ClientSingleton.inst().db.getAllCachedMessages();
            String[][] us = messages.stream().map(Cached_message_row::toStringList).toArray(String[][]::new);

            var jTable = new JTable(us, Cached_message_row.columnNames);
            jTable.getColumnModel().getColumn(0).setMaxWidth(18);
            jTable.getColumnModel().getColumn(1).setMaxWidth(125);
            jTable.getColumnModel().getColumn(1).setPreferredWidth(125);
            chatHistoryHolder.setViewportView(jTable);
        }

        localModelChanged();
    }


    private Channel getSelectedChanel() {
        return ClientSingleton.inst().db.getChannelByUuid(selectedChannelUuid);
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
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(10, 6, new Insets(10, 10, 10, 10), -1, -1));
        btnPullFromServer = new JButton();
        btnPullFromServer.setText("Pull From Server");
        contentPane.add(btnPullFromServer, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tableHolder = new JScrollPane();
        contentPane.add(tableHolder, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 8, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 100), null, 0, false));
        rightPanel = new JPanel();
        rightPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(rightPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 9, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        chatHistoryHolder = new JScrollPane();
        rightPanel.add(chatHistoryHolder, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Current user: ");
        contentPane.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 16), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("messages for event sourcing");
        contentPane.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("channels");
        contentPane.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createChanelButton = new JButton();
        createChanelButton.setText("Create Chanel");
        contentPane.add(createChanelButton, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelChatMessagesPane = new JScrollPane();
        contentPane.add(channelChatMessagesPane, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 6, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(8, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        contentPane.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(9, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setEnabled(true);
        sendButton.setText("Send");
        panel2.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageInput = new JTextField();
        panel2.add(messageInput, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(148, 30), null, 0, false));
        btnAcceptInvite = new JButton();
        btnAcceptInvite.setText("Membership pending, Join?");
        contentPane.add(btnAcceptInvite, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        txtChannelName = new JTextField();
        panel3.add(txtChannelName, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        renameChannelButton = new JButton();
        renameChannelButton.setText("Rename Channel");
        panel3.add(renameChannelButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoPullCheckBox = new JCheckBox();
        autoPullCheckBox.setSelected(true);
        autoPullCheckBox.setText("auto pull");
        contentPane.add(autoPullCheckBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentUser = new JLabel();
        currentUser.setText("Label");
        contentPane.add(currentUser, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(211, 16), null, 0, false));
        sendFileButton = new JButton();
        sendFileButton.setEnabled(true);
        sendFileButton.setText("Send file...");
        contentPane.add(sendFileButton, new com.intellij.uiDesigner.core.GridConstraints(8, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelsPane = new JScrollPane();
        contentPane.add(channelsPane, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 8, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        channelFileMessagesPane = new JScrollPane();
        contentPane.add(channelFileMessagesPane, new com.intellij.uiDesigner.core.GridConstraints(2, 5, 6, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
            var selectedChannel = getSelectedChanel();
            var mem = selectedChannel.getMember(user.facebook_id);
            if (mem != null)
                opt.memberStatus = mem.status;
            lst.add(opt);
        }
        return lst;
    }

    private void localModelChanged() {

        var newChecked = autoPullCheckBox.isSelected();
        if (newChecked != isAutoPulling) {
            isAutoPulling = newChecked;
            if (newChecked)
                autoPullCheckedTimer.start();
            else
                autoPullCheckedTimer.stop();
        }


        boolean sendButtonEnable = false;

        // swing doesn't allow to disable a panel :(
        if (selectedChannelUuid != null) {
            var selectedChannel = getSelectedChanel();

            // chat messages
            String[][] us = selectedChannel.chatMessages.stream().map(ChatMessage::toStringList).toArray(String[][]::new);
            var jTable = new JTable(us, ChatMessage.columnNames);
            channelChatMessagesPane.setViewportView(jTable);

            JScrollBar vertical = channelChatMessagesPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());

            // file messages
            var fm = selectedChannel.fileMessages;
            var listModel = new DefaultListModel<FileMessage>();
            listModel.addAll(fm);
            var jList = new JList<>(listModel);
            jList.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    var file = jList.getSelectedValue();
                    saveFileToHardDrive(file);
                }
            });

            channelFileMessagesPane.setViewportView(new JScrollPane(jList));
            jList.setCellRenderer(new FileRenderer());

            JScrollBar scrollFiles = channelFileMessagesPane.getVerticalScrollBar();
            scrollFiles.setValue(scrollFiles.getMaximum());


            var memberInChannel = selectedChannel.getMember(ClientSingleton.inst().db.facebook_id);
            btnAcceptInvite.setEnabled(memberInChannel.status == MemberStatus.INVITE_PENDING);
            if (selectedChannel.hasMember(memberInChannel.facebook_id))
                sendButtonEnable = true;

            var selectedUser = getSelectedUserFromDropdown();
            btnInviteUser.setEnabled(memberInChannel.status == MemberStatus.OWNER && selectedUser != null && selectedChannel.getMember(selectedUser.facebook_id) == null);

            btnRemoveUser.setEnabled(memberInChannel.status == MemberStatus.OWNER || (selectedUser != null && selectedUser.facebook_id == ClientSingleton.inst().db.facebook_id));

            renameChannelButton.setEnabled(memberInChannel.status == MemberStatus.OWNER);
            txtChannelName.setText(selectedChannel.name);
        }

        var inputText = messageInput.getText();
        if (inputText == null || inputText.equals("")) sendButtonEnable = false;

        if (sendButtonEnable) {
            sendButton.setEnabled(true);
            sendButton.setToolTipText("");
        } else {
            sendButton.setEnabled(false);
            sendButton.setToolTipText("Select a channel -AND- Type some text -AND- Be accepted in this channel");
        }
    }

    public void saveFileToHardDrive(FileMessage file) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("./"));

        int i = file.fileName.lastIndexOf('.');
        chooser.setSelectedFile(new File(file.fileName.substring(0, i)));

        String extensionNoDot = file.fileName.substring(i + 1);
        chooser.setFileFilter((new FileNameExtensionFilter(extensionNoDot + " file", extensionNoDot)));
        int actionDialog = chooser.showSaveDialog(this);

        if (actionDialog == JFileChooser.APPROVE_OPTION) {
            File tempPath = new File(chooser.getSelectedFile() + file.extension);
            try {
                writeFileAsBytes(tempPath.toString(), file.file);
                System.out.println("Saved file '" + file.fileName + "'to hard drive");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void writeFileAsBytes(String fullPath, String base64EncodedString) throws IOException {
        OutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fullPath));
        byte[] bytes = Util.base64(base64EncodedString);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        int token = -1;

        while ((token = inputStream.read()) != -1) {
            bufferedOutputStream.write(token);
        }
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        inputStream.close();
    }

}
