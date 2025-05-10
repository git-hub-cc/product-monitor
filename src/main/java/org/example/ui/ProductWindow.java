package org.example.ui;

import org.example.model.Product;
import org.example.service.ProductMonitor;
import org.example.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProductWindow extends JFrame implements ProductObserver , Logger.LogListener{
    private static final Logger logger = Logger.getInstance();
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final ProductMonitor monitor;
    private final JTextArea logArea;
    private final JLabel statusLabel;
    private final JLabel currentPriceLabel;
    private final JLabel targetPriceLabel;
    private final JButton actionButton;
    private JComboBox<Integer> frequencyComboBox;
    private final Map<String, StringBuilder> logBuffers = new HashMap<>();

    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        add(createStatusPanel(), BorderLayout.NORTH);
        add(createLogPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                setVisible(false);
            }
        });

        // 添加频率选择
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("请求频率(秒):"));

        Integer[] frequencies = {1, 2, 3, 5, 10};
        frequencyComboBox = new JComboBox<>(frequencies);
        frequencyComboBox.setSelectedItem(2); // 默认2秒
        frequencyComboBox.addActionListener(e -> updateMonitorFrequency());
        controlPanel.add(frequencyComboBox);

        add(controlPanel, BorderLayout.NORTH);

        // 注册日志监听
        Logger.getInstance().addListener(this);
    }

    private void updateMonitorFrequency() {
        int frequency = (Integer) frequencyComboBox.getSelectedItem();
        monitor.setMonitorFrequency(frequency * 1000L);
    }

    @Override
    public void onNewLog(String source, String message, Logger.LogLevel level, Date timestamp) {
        if (source.equals(monitor.getProduct().getName())) {
            SwingUtilities.invokeLater(() -> {
                appendLog(String.format("[%s][%s] %s",
                        TIME_FORMAT.format(timestamp),
                        level,
                        message
                ));
            });
        }
    }

    @Override
    public void dispose() {
        Logger.getInstance().removeListener(this);
        super.dispose();
    }


    public ProductWindow(String productName, ProductMonitor monitor) {
        super(productName + " - 发布日志");
        this.monitor = monitor;

        this.logArea = new JTextArea(20, 50);
        this.statusLabel = new JLabel("状态: 初始化");
        this.currentPriceLabel = new JLabel("当前价格: 未知");
        this.targetPriceLabel = new JLabel("目标价格: " +
                monitor.getProduct().getTargetPrice());
        this.actionButton = new JButton("停止监控");

        initializeUI();
    }


    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("状态信息"));

        panel.add(statusLabel);
        panel.add(currentPriceLabel);
        panel.add(targetPriceLabel);

        return panel;
    }

    private JScrollPane createLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return new JScrollPane(logArea);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        actionButton.addActionListener(e -> toggleMonitoring());
        panel.add(actionButton);

        JButton updatePriceButton = new JButton("修改价格");
        updatePriceButton.addActionListener(e -> updateTargetPrice());
        panel.add(updatePriceButton);

        JButton closeButton = new JButton("关闭窗口");
        closeButton.addActionListener(e -> setVisible(false));
        panel.add(closeButton);

        return panel;
    }

    private void toggleMonitoring() {
        if (monitor.isRunning()) {
            monitor.stopMonitoring();
            actionButton.setText("继续监控");
        } else {
            monitor.startMonitoring();
            actionButton.setText("停止监控");
        }
    }

    private void updateTargetPrice() {
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
                    showError("价格必须大于0！");
                    return;
                }

                monitor.updateTargetPrice(newPrice);
                targetPriceLabel.setText("目标价格: " + newPrice);
                logger.log(monitor.getProduct().getName(),
                        "更新目标价格: " + newPrice);
            } catch (NumberFormatException e) {
                showError("请输入有效的价格！");
            }
        }
    }

    @Override
    public void update(Product product, String message) {
        SwingUtilities.invokeLater(() -> {
            updateLabels(product);
            appendLog(message);
        });
    }

    private void updateLabels(Product product) {
        statusLabel.setText("状态: " + product.getStatus());
        currentPriceLabel.setText("当前价格: " + product.getCurrentPrice());
    }

    private void appendLog(String message) {
        String timeStamp = TIME_FORMAT.format(new Date());
        logArea.append(String.format("[%s] %s%n", timeStamp, message));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误",
                JOptionPane.ERROR_MESSAGE);
    }
}