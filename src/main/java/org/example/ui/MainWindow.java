package org.example.ui;

import org.example.service.ProductMonitor;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainWindow extends JFrame {
    private final Map<String, ProductMonitor> monitors = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final JTextField nameField = new JTextField(20);
    private final JTextField priceField = new JTextField(10);
    private final JPanel productListPanel = new JPanel(new GridLayout(0, 1, 5, 5));

    public MainWindow() {
        setTitle("商品抢购系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 创建添加商品面板
        JPanel addPanel = createAddPanel();

        // 创建商品列表面板
        JScrollPane scrollPane = new JScrollPane(productListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("监控商品列表"));

        add(addPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        pack();
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createAddPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("添加商品"));

        panel.add(new JLabel("商品名称:"));
        panel.add(nameField);
        panel.add(new JLabel("目标价格:"));
        panel.add(priceField);

        JButton addButton = new JButton("添加监控");
        addButton.addActionListener(e -> addProduct());
        panel.add(addButton);

        return panel;
    }

    private void addProduct() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入商品名称和价格！");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "价格必须大于0！");
                return;
            }

            ProductMonitor monitor = monitors.get(name);
            if (monitor != null) {
                int option = JOptionPane.showConfirmDialog(this,
                        "商品已存在，是否更新价格？",
                        "确认更新",
                        JOptionPane.YES_NO_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    monitor.stopMonitoring();
                    monitors.remove(name);
                } else {
                    return;
                }
            }

            monitor = new ProductMonitor(name, price);
            ProductWindow window = new ProductWindow(name, monitor, this);
            monitor.addObserver(window);

            monitors.put(name, monitor);
            executor.submit(monitor::startMonitoring);

            updateProductList();

            // 清空输入框
            nameField.setText("");
            priceField.setText("");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的价格！");
        }
    }

    public void removeProduct(String name) {
        ProductMonitor monitor = monitors.remove(name);
        if (monitor != null) {
            monitor.stopMonitoring();
            updateProductList();
        }
    }

    private void updateProductList() {
        productListPanel.removeAll();
        monitors.forEach((name, monitor) -> {
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            itemPanel.add(new JLabel(name + " - 目标价格: " + monitor.getProduct().getTargetPrice()));

            JButton showButton = new JButton("显示窗口");
            showButton.addActionListener(e -> monitor.showWindow());
            itemPanel.add(showButton);

            productListPanel.add(itemPanel);
        });
        productListPanel.revalidate();
        productListPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow());
    }
}