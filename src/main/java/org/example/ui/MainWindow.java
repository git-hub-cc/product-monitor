package org.example.ui;

import org.example.model.AddressItem;
import org.example.service.ProductMonitor;
import org.example.service.ProductRelease;
import org.example.service.UserLogin;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主窗口类，用于展示商品监控系统的主界面
 * 实现了用户登录、商品的添加、移除和监控功能
 */
public class MainWindow extends JFrame {
    // 存储商品监控器的线程安全Map
    private final Map<String, ProductMonitor> monitors = new ConcurrentHashMap<>();
    private final ProductRelease productRelease = new ProductRelease();
    private final UserLogin userLogin = new UserLogin();

    public static JComboBox<AddressItem> addressComboBox;

    // 用于执行监控任务的线程池
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // UI组件
    private final JTextField nameField = new JTextField(20);
    private final JTextField priceField = new JTextField(10);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField codeField = new JTextField(15);
    private final JPanel productListPanel = new JPanel();
    private final JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
    private final JPanel loginPanel;

    /**
     * 构造函数：初始化主窗口界面
     */
    public MainWindow() {
        setTitle("商品抢购系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 初始化登录面板
        loginPanel = createLoginPanel();

        // 初始化商品列表面板的布局
        productListPanel.setLayout(new BoxLayout(productListPanel, BoxLayout.Y_AXIS));

        // 初始化主内容面板
        initializeMainContentPanel();

        // 初始显示登录面板
        add(loginPanel, BorderLayout.CENTER);

        // 设置窗口属性
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * 初始化主内容面板
     */
    private void initializeMainContentPanel() {
        mainContentPanel.setVisible(false);

        // 创建添加商品面板
        JPanel addPanel = createAddPanel();

        // 创建带滚动条的商品列表面板
        JScrollPane scrollPane = new JScrollPane(productListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("监控商品列表"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 添加面板到主内容面板
        mainContentPanel.add(addPanel, BorderLayout.NORTH);
        mainContentPanel.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 创建登录面板
     */
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 手机号输入
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("手机号:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);

        // 验证码输入
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("验证码:"), gbc);
        gbc.gridx = 1;
        panel.add(codeField, gbc);

        // 登录按钮
        JButton loginButton = new JButton("登录");
        gbc.gridx = 1; gbc.gridy = 2;
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton, gbc);

        return panel;
    }

    /**
     * 处理登录操作
     */
    private void handleLogin() {
        String phone = phoneField.getText().trim();
        String code = codeField.getText().trim();

        if (phone.isEmpty() || code.isEmpty()) {
            showErrorMessage("请输入手机号和验证码！");
            return;
        }

        try {
            if (userLogin.login(phone, code)) {
                // 登录成功，切换到主界面
                remove(loginPanel);
                add(mainContentPanel);
                mainContentPanel.setVisible(true);
                revalidate();
                repaint();

                // 显示欢迎信息
                JOptionPane.showMessageDialog(this,
                        "欢迎回来，" + userLogin.getNickName());
                List<AddressItem> addresses = userLogin.getAddresses();
                // 更新地址选择下拉框
                updateAddressComboBox(addresses);

                revalidate();
                repaint();
            } else {
                showErrorMessage("登录失败，请检查手机号和验证码是否正确");
            }
        } catch (Exception e) {
            showErrorMessage("登录失败: " + e.getMessage());
        }
    }

    /**
     * 创建添加商品的面板
     */
    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("添加商品"));

        // 添加输入组件
        panel.add(new JLabel("商品名称:"));
        panel.add(nameField);
        panel.add(new JLabel("目标价格:"));
        panel.add(priceField);


        // 添加地址选择下拉框
        addressComboBox = new JComboBox<>();
        panel.add(new JLabel("选择地址:"));
        panel.add(addressComboBox);

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
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }


    /**
     * 更新地址选择下拉框
     */
    private void updateAddressComboBox(List<AddressItem> addresses) {
        addressComboBox.removeAllItems();
        for (AddressItem address : addresses) {
            addressComboBox.addItem(address);
        }
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
        SwingUtilities.invokeLater(MainWindow::new);
    }
}