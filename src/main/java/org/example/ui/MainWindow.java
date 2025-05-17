package org.example.ui;

import org.example.service.ProductMonitor;
import org.example.service.ProductRelease;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主窗口类，用于展示商品监控系统的主界面
 * 实现了商品的添加、移除和监控功能
 */
public class MainWindow extends JFrame {
    // 存储商品监控器的线程安全Map
    private final Map<String, ProductMonitor> monitors = new ConcurrentHashMap<>();
    private final ProductRelease productRelease = new ProductRelease();
    // 用于执行监控任务的线程池
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // UI组件
    private final JTextField nameField = new JTextField(20);
    private final JTextField priceField = new JTextField(10);
    // 使用普通JPanel，布局将在构造函数中设置
    private final JPanel productListPanel = new JPanel();

    /**
     * 构造函数：初始化主窗口界面
     */
    public MainWindow() {
        setTitle("商品抢购系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 初始化商品列表面板的布局
        productListPanel.setLayout(new BoxLayout(productListPanel, BoxLayout.Y_AXIS));

        // 创建添加商品面板
        JPanel addPanel = createAddPanel();

        // 创建带滚动条的商品列表面板
        JScrollPane scrollPane = new JScrollPane(productListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("监控商品列表"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 添加面板到主窗口
        add(addPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 设置窗口属性
        pack();
        setSize(800, 600);
        setLocationRelativeTo(null); // 窗口居中显示
        setVisible(true);
    }

    /**
     * 创建添加商品的面板
     * @return 配置好的JPanel
     */
    private JPanel createAddPanel() {
        // 使用FlowLayout，设置左对齐和组件间距
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("添加商品"));

        // 添加输入组件
        panel.add(new JLabel("商品名称:"));
        panel.add(nameField);
        panel.add(new JLabel("目标价格:"));
        panel.add(priceField);

        // 添加 购买 按钮
        JButton addButton = new JButton("购买");
        addButton.addActionListener(e -> addProduct());
        panel.add(addButton);

        // 添加 发布预购 按钮
        JButton preOrderButton = new JButton("发布预购");
        preOrderButton.addActionListener(e -> publishPreOrder());
        panel.add(preOrderButton);

        return panel;
    }

    /**
     * 添加新商品到监控列表
     */
    private void addProduct() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        // 输入验证
        if (name.isEmpty() || priceText.isEmpty()) {
            showErrorMessage("请输入商品名称和价格！");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                showErrorMessage("价格必须大于0！");
                return;
            }

            // 检查商品是否已存在
            ProductMonitor monitor = monitors.get(name);
            if (monitor != null) {
                handleExistingProduct(name, monitor);
                return;
            }

            // 创建新的商品监控
            createNewProductMonitor(name, price);

            // 清空输入框
            clearInputFields();

        } catch (NumberFormatException e) {
            showErrorMessage("请输入有效的价格！");
        }
    }

    /**
     * 处理已存在的商品
     */
    private void handleExistingProduct(String name, ProductMonitor monitor) {
        int option = JOptionPane.showConfirmDialog(this,
                "商品已存在，是否更新价格？",
                "确认更新",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            monitor.stopMonitoring();
            monitors.remove(name);
        }
    }

    /**
     * 创建新的商品监控器
     */
    private void createNewProductMonitor(String name, double price) {
        ProductMonitor monitor = new ProductMonitor(name, price);
        ProductWindow window = new ProductWindow(name, monitor, this);
        monitor.addObserver(window);

        monitors.put(name, monitor);
        executor.submit(monitor::startMonitoring);

        updateProductList();
    }


    /**
     * 发布预购功能
     */
    private void publishPreOrder() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        // 输入验证
        if (name.isEmpty() || priceText.isEmpty()) {
            showErrorMessage("请输入商品名称和价格！");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                showErrorMessage("价格必须大于0！");
                return;
            }

            boolean success = productRelease.publishPreOrder(name, price);
            if (success) {
                JOptionPane.showMessageDialog(this, "预购发布成功！");
                clearInputFields();
            } else {
                showErrorMessage("预购确认失败！");
            }

        } catch (NumberFormatException e) {
            showErrorMessage("请输入有效的价格！");
        } catch (Exception e) {
            showErrorMessage("发布预购失败: " + e.getMessage());
        }
    }
    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /**
     * 清空输入框
     */
    private void clearInputFields() {
        nameField.setText("");
        priceField.setText("");
    }

    /**
     * 从监控列表中移除商品
     */
    public void removeProduct(String name) {
        ProductMonitor monitor = monitors.remove(name);
        if (monitor != null) {
            monitor.stopMonitoring();
            updateProductList();
        }
    }

    /**
     * 更新商品列表显示
     */
    private void updateProductList() {
        productListPanel.removeAll();

        // 创建一个面板作为容器，使用垂直方向的BoxLayout
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        // 设置容器面板的对齐方式
        containerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 添加一些上边距
        containerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        monitors.forEach((name, monitor) -> {
            // 创建单个商品项的面板
            JPanel itemPanel = new JPanel();
            itemPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0)); // 设置水平间距为5，垂直间距为0
            // 关键：设置首选尺寸和最大尺寸
            itemPanel.setMaximumSize(new Dimension(productListPanel.getWidth(), 35)); // 设置固定高度
            itemPanel.setPreferredSize(new Dimension(productListPanel.getWidth(), 35));
            itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // 创建标签和按钮
            JLabel label = new JLabel(String.format("%s - 目标价格: %.2f",
                    name, monitor.getProduct().getTargetPrice()));
            JButton showButton = new JButton("显示窗口");
            showButton.addActionListener(e -> monitor.showWindow());

            // 添加组件到商品面板
            itemPanel.add(label);
            itemPanel.add(showButton);

            // 添加商品面板和间隔到容器
            containerPanel.add(itemPanel);
            containerPanel.add(Box.createVerticalStrut(10)); // 添加10像素的垂直间隔
        });

        // 如果有商品，移除最后一个间隔
        if (!monitors.isEmpty()) {
            containerPanel.remove(containerPanel.getComponentCount() - 1);
        }

        // 添加一个面板作为"胶水"，填充剩余空间
        containerPanel.add(Box.createVerticalGlue());

        // 将容器面板添加到滚动面板中
        productListPanel.add(containerPanel);

        // 刷新UI
        productListPanel.revalidate();
        productListPanel.repaint();
    }
    /**
     * 程序入口点
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow());
    }
}