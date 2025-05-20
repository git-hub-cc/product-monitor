package org.example.ui;

import org.example.config.Config;
import org.example.model.AddressItem;
import org.example.model.ProductListItem;
import org.example.service.*;
import org.example.service.strategy.BuyOperation;
import org.example.service.strategy.PreOrderOperation;
import org.example.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.scene.layout.VBox;
import javafx.concurrent.Worker;

public class MainWindow extends JFrame {
    private static final Logger logger = Logger.getInstance();
    private final Config config = Config.getInstance();
    private final UserLogin userLogin = UserLogin.getInstance();
    private final Map<String, ProductMonitor> monitors = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // UI组件声明
    public static JComboBox<AddressItem> addressComboBox;
    private final JTextField nameField;
    private final JTextField priceField;
    private final JTextField phoneField;
    private final JTextField codeField;
    private final JPanel productListPanel;
    private final JPanel mainContentPanel;
    private final JPanel loginPanel;
    private final DefaultListModel<ProductListItem> productListModel;
    private final JList<ProductListItem> productList;

    // 网页预览相关组件
    private final JTextField urlField;
    private final JPanel webPreviewPanel;
    private WebView webView;
    private final JFXPanel jfxPanel;
    private final int PREVIEW_WIDTH = 414;  // iPhone标准宽度
    private final int PREVIEW_HEIGHT = 896; // iPhone标准高度
    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("商品管理"));

        // 创建输入面板
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("商品名称:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("目标价格:"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("选择地址:"));
        inputPanel.add(addressComboBox);

        // 添加商品按钮
        JButton addButton = new JButton("添加商品");
        addButton.addActionListener(e -> addProduct());
        inputPanel.add(addButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        // 创建商品列表面板
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("商品列表"));

        // 设置商品列表
        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(productList);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建操作按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton buyButton = new JButton("开始购买");
        JButton preOrderButton = new JButton("发布预购");
        JButton removeButton = new JButton("移除商品");

        buyButton.addActionListener(e -> startBuyingSelected());
        preOrderButton.addActionListener(e -> startPreOrderSelected());
        removeButton.addActionListener(e -> removeSelectedProducts());

        buttonPanel.add(buyButton);
        buttonPanel.add(preOrderButton);
        buttonPanel.add(removeButton);

        listPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(listPanel, BorderLayout.CENTER);

        return panel;
    }
    private void addProduct() {
        try {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();

            if (name.isEmpty() || priceText.isEmpty()) {
                showError("请输入商品名称和价格");
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceText);
                if (price <= 0) {
                    showError("价格必须大于0");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("请输入有效的价格");
                return;
            }

            // 检查商品是否已存在
            boolean exists = false;
            for (int i = 0; i < productListModel.getSize(); i++) {
                if (productListModel.getElementAt(i).getName().equals(name)) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                showError("商品已存在");
                return;
            }

            // 创建并添加商品项
            ProductListItem item = new ProductListItem(name, price);
            productListModel.addElement(item);

            // 创建监控器
            ProductMonitor monitor = ProductMonitorFactory.createMonitor(name, price);
            monitors.put(name, monitor);

            // 清空输入框
            nameField.setText("");
            priceField.setText("");

            // 记录日志
            logger.log("System", String.format("添加商品: %s, 价格: %.2f", name, price),
                    Logger.LogLevel.INFO);

            // 显示成功消息
            showMessage("商品添加成功");

        } catch (Exception e) {
            logger.log("System", "添加商品失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            showError("添加商品失败: " + e.getMessage());
        }
    }

    private void startBuyingSelected() {
        List<ProductListItem> selectedItems = productList.getSelectedValuesList();
        if (selectedItems.isEmpty()) {
            showError("请选择要购买的商品");
            return;
        }

        // 创建购买模式选择对话框
        Object[] options = {"单次购买", "持续购买", "取消"};
        int choice = JOptionPane.showOptionDialog(this,
                "请选择购买模式",
                "购买模式",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == JOptionPane.CANCEL_OPTION) {
            return;
        }

        // 更新配置
        boolean continuousBuying = (choice == JOptionPane.NO_OPTION); // NO_OPTION对应"持续购买"
        config.set("CONTINUOUS_BUYING", continuousBuying ? "1" : "0");
        config.saveConfig();

        // 启动购买任务
        for (ProductListItem item : selectedItems) {
            ProductMonitor monitor = monitors.get(item.getName());
            if (monitor != null) {
                monitor.setOperation(new BuyOperation(continuousBuying));
                ProductWindow window = new ProductWindow(item.getName(), monitor);
                monitor.addObserver(window);
                window.setVisible(true);
                executor.submit(monitor::startMonitoring);

                String modeStr = continuousBuying ? "持续购买" : "单次购买";
                logger.log("System",
                        String.format("开始%s商品: %s, 目标价格: %.2f",
                                modeStr, item.getName(), item.getTargetPrice()),
                        Logger.LogLevel.INFO);
            }
        }
    }

    private void removeSelectedProducts() {
        List<ProductListItem> selectedItems = productList.getSelectedValuesList();
        if (selectedItems.isEmpty()) {
            showError("请选择要移除的商品");
            return;
        }

        for (ProductListItem item : selectedItems) {
            ProductMonitor monitor = monitors.remove(item.getName());
            if (monitor != null) {
                monitor.stopMonitoring();
                productListModel.removeElement(item);
                logger.log("System", "移除商品: " + item.getName(), Logger.LogLevel.INFO);
            }
        }
    }


    private void startPreOrderSelected() {
        List<ProductListItem> selectedItems = productList.getSelectedValuesList();
        if (selectedItems.isEmpty()) {
            showError("请选择要预购的商品");
            return;
        }

        PreOrderOperation.PublishMode mode = showPublishModeDialog();
        if (mode != null) {
            for (ProductListItem item : selectedItems) {
                ProductMonitor monitor = monitors.get(item.getName());
                if (monitor != null) {
                    monitor.setPreOrderOperation(mode);
                    ProductWindow window = new ProductWindow(item.getName(), monitor);
                    monitor.addObserver(window);
                    window.setVisible(true);
                    executor.submit(monitor::startMonitoring);
                    logger.log("System", String.format("开始预购商品: %s, 模式: %s",
                            item.getName(), mode.getDescription()), Logger.LogLevel.INFO);
                }
            }
        }
    }

        private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    message,
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            logger.log("System", "错误: " + message, Logger.LogLevel.ERROR);
        });
    }

    private void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    message,
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            logger.log("System", message, Logger.LogLevel.INFO);
        });
    }


    public MainWindow() {
        super("商品抢购系统");

        // 初始化JavaFX平台
        Platform.setImplicitExit(false);

        // 初始化UI组件
        this.nameField = new JTextField(20);
        this.priceField = new JTextField(10);
        this.phoneField = new JTextField(15);
        this.codeField = new JTextField(15);
        this.productListPanel = new JPanel();
        this.mainContentPanel = new JPanel(new BorderLayout(10, 10));
        addressComboBox = new JComboBox<>();
        this.loginPanel = createLoginPanel();
        this.productListModel = new DefaultListModel<>();
        this.productList = new JList<>(productListModel);

        // 初始化网页预览组件
        this.urlField = new JTextField(30);
        this.webPreviewPanel = new JPanel();
        this.jfxPanel = new JFXPanel();

        try {
            initializeUI();
            initializeWebPreview();
            checkLoginStatus();

            // 设置窗口属性
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(800, 1250);
            setLocationRelativeTo(null);

            logger.log("System", "窗口初始化成功", Logger.LogLevel.INFO);
        } catch (Exception e) {
            logger.log("System", "窗口初始化失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            throw new RuntimeException("窗口初始化失败", e);
        }
    }

    private void initializeWebPreview() {
        // 创建URL输入面板
        JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        urlPanel.add(new JLabel("网址:"));
        urlPanel.add(urlField);

        JButton previewButton = new JButton("预览");
        previewButton.addActionListener(e -> loadWebPreview());
        urlPanel.add(previewButton);

        // 设置JFXPanel的首选大小
        jfxPanel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));

        // 在JavaFX线程中初始化WebView
        Platform.runLater(() -> {
            try {
                webView = new WebView();
                webView.setPrefSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

                // 启用JavaScript
                webView.getEngine().setJavaScriptEnabled(true);

                // 设置用户代理为移动设备
                webView.getEngine().setUserAgent(
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 " +
                                "(KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                );

                // 创建场景
                VBox root = new VBox();
                root.getChildren().add(webView);
                Scene scene = new Scene(root, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                jfxPanel.setScene(scene);

                // 添加加载状态监听器，在页面加载完成后设置token
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED && config.get("XM_URL").equals(urlField.getText())) {
                        setWebToken();
                    }
                });

                // 加载初始页面
                webView.getEngine().load(config.get("XM_URL"));
            } catch (Exception e) {
                logger.log("System", "WebView初始化失败: " + e.getMessage(), Logger.LogLevel.ERROR);
                SwingUtilities.invokeLater(() -> showError("WebView初始化失败: " + e.getMessage()));
            }
        });

        // 创建一个带滚动条的面板来容纳预览窗口
        JScrollPane scrollPane = new JScrollPane(jfxPanel);
        scrollPane.setPreferredSize(new Dimension(PREVIEW_WIDTH + 20, PREVIEW_HEIGHT + 20));

        // 设置预览面板
        webPreviewPanel.setLayout(new BorderLayout());
        webPreviewPanel.add(urlPanel, BorderLayout.NORTH);
        webPreviewPanel.add(scrollPane, BorderLayout.CENTER);
        webPreviewPanel.setBorder(BorderFactory.createTitledBorder("手机网页预览"));

        // 添加到主面板
        mainContentPanel.add(webPreviewPanel, BorderLayout.EAST);
    }

    private void loadWebPreview() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showError("请输入网址");
            return;
        }

        // 添加协议前缀如果没有的话
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        final String finalUrl = url;
        Platform.runLater(() -> {
            try {
                webView.getEngine().load(finalUrl);
            } catch (Exception e) {
                logger.log("System", "加载网页失败: " + e.getMessage(), Logger.LogLevel.ERROR);
                SwingUtilities.invokeLater(() -> showError("加载网页失败: " + e.getMessage()));
            }
        });
    }



    private void setWebToken() {
        String token = config.get(Config.USER_TOKEN);
        if (token == null || token.isEmpty()) {
            showError("未找到token，请先登录");
            return;
        }

        Platform.runLater(() -> {
            try {
                // 注入JavaScript代码来设置token
                String script = String.format(
                        "localStorage.setItem('token', '%s'); " +
                                "localStorage.setItem('initToken', '%s'); " +
                                "localStorage.setItem('initInfo', '%s'); " +
                                "localStorage.setItem('userInfo', '%s'); " +
                                "document.cookie = 'token=%s; path=/'; " +
                                "console.log('Token and user info set successfully');",
                        token, token,
                        config.get("LOGIN_INFO"),
                        config.get("LOGIN_INFO"),
                        token
                );

                webView.getEngine().executeScript(script);
            } catch (Exception e) {
                logger.log("System", "设置Token失败: " + e.getMessage(), Logger.LogLevel.ERROR);
                showError("设置Token失败: " + e.getMessage());
            }
        });
    }

    private void initializeUI() {
        // 设置布局
        setLayout(new BorderLayout());

        // 初始化产品列表面板
        productListPanel.setLayout(new BoxLayout(productListPanel, BoxLayout.Y_AXIS));

        // 初始化地址下拉框并添加点击监听器
        addressComboBox = new JComboBox<>();
        addressComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                updateAddressList();
            }
        });

        // 初始化主内容面板
        mainContentPanel.setVisible(false);
        mainContentPanel.add(createAddPanel(), BorderLayout.NORTH);
        mainContentPanel.add(createProductListScrollPane(), BorderLayout.CENTER);

        // 设置产品列表属性
        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 添加窗口关闭监听
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleWindowClosing();
            }
        });
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 添加手机号输入
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("手机号:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);

        // 添加验证码输入
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("验证码:"), gbc);
        gbc.gridx = 1;
        panel.add(codeField, gbc);

        // 添加登录按钮
        JButton loginButton = new JButton("登录");
        gbc.gridx = 1; gbc.gridy = 2;
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton, gbc);

        return panel;
    }

    private void checkLoginStatus() {
        if (config.isUserLoggedIn()) {
            handleAutoLogin();
        } else {
            getContentPane().add(loginPanel, BorderLayout.CENTER);
        }
    }

    private void handleAutoLogin() {
        try {
            String token = config.get(Config.USER_TOKEN);
            String nickName = config.get(Config.USER_NICKNAME);
            String phone = config.get(Config.USER_PHONE);

            userLogin.setLoginInfo(token, nickName, phone);
            switchToMainContent();
            updateAddressList();

            logger.log("System", "自动登录成功: " + nickName, Logger.LogLevel.INFO);
            showMessage("欢迎回来，" + nickName);
        } catch (Exception e) {
            logger.log("System", "自动登录失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            handleLoginFailure(e);
        }
    }

    private void switchToMainContent() {
        getContentPane().removeAll();
        getContentPane().add(mainContentPanel, BorderLayout.CENTER);
        mainContentPanel.setVisible(true);
        revalidate();
        repaint();
    }


    private PreOrderOperation.PublishMode showPublishModeDialog() {
        Object[] options = {
                PreOrderOperation.PublishMode.SINGLE.getDescription(),
                PreOrderOperation.PublishMode.TRIPLE.getDescription(),
                "取消"
        };

        int choice = JOptionPane.showOptionDialog(this,
                "请选择发布模式",
                "发布预购",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            return PreOrderOperation.PublishMode.SINGLE;
        } else if (choice == JOptionPane.NO_OPTION) {
            return PreOrderOperation.PublishMode.TRIPLE;
        }

        return null;
    }


    private void handleWindowClosing() {
        monitors.values().forEach(ProductMonitor::stopMonitoring);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.shutdown();
        config.saveConfig();
        ProductMonitorFactory.shutdown();
    }


    private JScrollPane createProductListScrollPane() {
        JScrollPane scrollPane = new JScrollPane(productListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("监控商品列表"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }



    private void handleLogin() {
        String phone = phoneField.getText().trim();
        String code = codeField.getText().trim();

        if (!validateLoginInput(phone, code)) {
            return;
        }

        try {
            if (userLogin.login(phone, code)) {
                handleSuccessfulLogin();
            } else {
                showError("登录失败，请检查手机号和验证码");
            }
        } catch (Exception e) {
            handleLoginFailure(e);
        }
    }

    private boolean validateLoginInput(String phone, String code) {
        if (phone.isEmpty() || code.isEmpty()) {
            showError("请输入手机号和验证码");
            return false;
        }
        return true;
    }

    private void handleSuccessfulLogin() {
        saveLoginInfo();
        switchToMainContent();
        updateAddressList();
        showMessage("登录成功，欢迎 " + userLogin.getNickName());
    }

    private void saveLoginInfo() {
        config.set(Config.USER_TOKEN, userLogin.getToken());
        config.set(Config.USER_NICKNAME, userLogin.getNickName());
        config.set(Config.USER_PHONE, userLogin.getPhone());
        config.saveConfig();
    }

    private void updateAddressList() {
        try {
            List<AddressItem> addresses = userLogin.getAddresses();
            updateAddressComboBox(addresses);
        } catch (Exception e) {
            logger.log("System", "获取地址列表失败: " + e.getMessage(), Logger.LogLevel.ERROR);
            showError("获取地址列表失败: " + e.getMessage());
        }
    }

    private void handleLoginFailure(Exception e) {
        userLogin.logout();
        showError("登录失败: " + e.getMessage());
        add(loginPanel, BorderLayout.CENTER);
    }


    private void updateAddressComboBox(List<AddressItem> addresses) {
        addressComboBox.removeAllItems();
        for (AddressItem address : addresses) {
            addressComboBox.addItem(address);
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.log("System", "设置系统外观失败: " + e.getMessage(), Logger.LogLevel.WARN);
        }

        // 确保在JavaFX线程初始化之前启动
        Platform.startup(() -> {});

        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setVisible(true);
                logger.log("System", "程序启动成功", Logger.LogLevel.INFO);
            } catch (Exception e) {
                logger.log("System", "程序启动失败: " + e.getMessage(), Logger.LogLevel.ERROR);
                JOptionPane.showMessageDialog(null,
                        "程序启动失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}