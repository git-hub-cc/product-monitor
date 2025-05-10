package org.example.ui;

import org.example.model.Product;
import org.example.service.ProductMonitor;
import org.example.service.TaskStatus;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;



/**
 * 商品监控窗口类
 * 用于显示商品价格监控的状态、日志和控制界面
 */
public class ProductWindow extends JFrame implements ProductObserver {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    // UI 组件
    private final JTextArea logArea;          // 日志显示区域
    private final ProductMonitor monitor;      // 商品监控器
    private JLabel statusLabel;         // 状态标签
    private JLabel currentPriceLabel;   // 当前价格标签
    private JLabel targetPriceLabel;    // 目标价格标签
    private JButton actionButton;       // 动作按钮（开始/停止）
    private JButton updatePriceButton;  // 更新价格按钮

    /**
     * 构造函数
     * @param productName 商品名称
     * @param monitor 商品监控器
     */
    public ProductWindow(String productName, ProductMonitor monitor, MainWindow mainWindow) {
        super(productName + " - 抢购日志");
        this.monitor = monitor;
        this.statusLabel = statusLabel;
        this.currentPriceLabel = currentPriceLabel;
        this.targetPriceLabel = targetPriceLabel;
        this.actionButton = actionButton;
        this.updatePriceButton = updatePriceButton;

        // 创建主面板，使用BorderLayout布局
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 初始化各个面板
        JPanel statusPanel = createStatusPanel();
        logArea = createLogArea();
        JPanel buttonPanel = createButtonPanel();

        // 组装主面板
        mainPanel.add(statusPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 配置窗口属性
        add(mainPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 添加窗口关闭监听器
        setupWindowListener();
    }

    /**
     * 创建状态面板
     */
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

    /**
     * 创建日志显示区域
     */
    private JTextArea createLogArea() {
        JTextArea area = new JTextArea(20, 50);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    /**
     * 创建按钮面板
     */
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

    /**
     * 设置窗口监听器
     */
    private void setupWindowListener() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                setVisible(false);
            }
        });
    }

    /**
     * 切换监控状态
     */
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

    /**
     * 更新目标价格
     */
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

    /**
     * 实现ProductObserver接口的更新方法
     * @param product 商品对象
     * @param message 更新消息
     */
    @Override
    public void update(Product product, String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: " + product.getStatus());
            currentPriceLabel.setText("当前价格: " + product.getCurrentPrice());

            // 添加时间戳的日志记录
            logArea.append(String.format("[%s] %s%n",
                    TIME_FORMAT.format(new Date()),
                    message));
            // 自动滚动到最新日志
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}