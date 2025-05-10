package org.example.ui;

import org.example.model.Product;
import org.example.service.ProductMonitor;
import org.example.service.TaskStatus;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProductWindow extends JFrame implements ProductObserver {
    private JTextArea logArea;
    private ProductMonitor monitor;
    private JLabel statusLabel;
    private JLabel currentPriceLabel;
    private JLabel targetPriceLabel;
    private JButton actionButton;
    private JButton updatePriceButton;

    public ProductWindow(String productName, ProductMonitor monitor, MainWindow mainWindow) {
        super(productName + " - 抢购日志");
        this.monitor = monitor;

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建状态面板
        JPanel statusPanel = createStatusPanel();

        // 创建日志区域
        logArea = new JTextArea(20, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(statusPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                setVisible(false);
            }
        });
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("状态信息"));

        statusLabel = new JLabel("状态: 初始化");
        currentPriceLabel = new JLabel("当前价格: 未知");
        targetPriceLabel = new JLabel("目标价格: " + monitor.getProduct().getTargetPrice());

        panel.add(statusLabel);
        panel.add(currentPriceLabel);
        panel.add(targetPriceLabel);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        actionButton = new JButton("停止监控");
        actionButton.addActionListener(e -> toggleMonitoring());

        updatePriceButton = new JButton("修改价格");
        updatePriceButton.addActionListener(e -> updatePrice());

        JButton closeButton = new JButton("关闭窗口");
        closeButton.addActionListener(e -> setVisible(false));

        panel.add(actionButton);
        panel.add(updatePriceButton);
        panel.add(closeButton);

        return panel;
    }

    private void toggleMonitoring() {
        if (monitor.getStatus() == TaskStatus.RUNNING) {
            monitor.stopMonitoring();
            actionButton.setText("继续监控");
            updatePriceButton.setEnabled(true);
        } else {
            monitor.startMonitoring();
            actionButton.setText("停止监控");
            updatePriceButton.setEnabled(false);
        }
    }

    private void updatePrice() {
        String input = JOptionPane.showInputDialog(
                this,
                "请输入新的目标价格:",
                "修改价格",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                double newPrice = Double.parseDouble(input.trim());
                if (newPrice <= 0) {
                    JOptionPane.showMessageDialog(this, "价格必须大于0！");
                    return;
                }

                monitor.updateTargetPrice(newPrice);
                targetPriceLabel.setText("目标价格: " + newPrice);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效的价格！");
            }
        }
    }

    @Override
    public void update(Product product, String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: " + product.getStatus());
            currentPriceLabel.setText("当前价格: " + product.getCurrentPrice());

            logArea.append(String.format("[%s] %s%n",
                    new SimpleDateFormat("HH:mm:ss").format(new Date()),
                    message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}