package com.example.swaggercoverage;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Set;

/**
 * The "Swagger Coverage" suite tab.
 */
public final class CoverageTab {

    private final MontoyaApi api;
    private final EndpointRegistry registry;
    private final JPanel panel;
    private final DefaultTableModel tableModel;
    private final JLabel summaryLabel;
    private final JProgressBar progressBar;

    public CoverageTab(MontoyaApi api, EndpointRegistry registry) {
        this.api = api;
        this.registry = registry;
        this.panel = new JPanel(new BorderLayout(8, 8));

        // -- Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton loadBtn = new JButton("Load OpenAPI/Swagger Spec...");
        loadBtn.addActionListener(e -> loadSpec());

        JButton reanalyzeBtn = new JButton("Re-analyze Site Map");
        reanalyzeBtn.addActionListener(e -> {
            int n = SiteMapAnalyzer.analyze(api, registry);
            api.logging().logToOutput("Site map re-analysis: " + n + " requests processed.");
            refresh();
        });

        JButton resetBtn = new JButton("Reset Observed");
        resetBtn.addActionListener(e -> { registry.reset(); refresh(); });

        toolbar.add(loadBtn);
        toolbar.add(reanalyzeBtn);
        toolbar.add(resetBtn);

        // -- Summary
        summaryLabel = new JLabel("No spec loaded.");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(summaryLabel, BorderLayout.WEST);
        summaryPanel.add(progressBar, BorderLayout.CENTER);

        // -- Table
        tableModel = new DefaultTableModel(
                new Object[]{"Status", "Method", "Path"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(560);
        table.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(table);

        // -- Assemble: summary on top, table fills the rest
        JPanel top = new JPanel(new BorderLayout());
        top.add(toolbar, BorderLayout.NORTH);
        top.add(summaryPanel, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        refresh();
    }

    private void loadSpec() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select OpenAPI/Swagger spec");
        if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        try {
            Set<Endpoint> endpoints = SpecParser.parse(path);
            registry.setDocumented(endpoints);
            api.logging().logToOutput("Loaded " + endpoints.size()
                    + " endpoints from " + path);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel,
                    "Failed to parse spec:\n" + ex.getMessage(),
                    "Spec Error", JOptionPane.ERROR_MESSAGE);
            api.logging().logToError("Spec parse failed: " + ex.getMessage());
        }
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);

            Set<Endpoint> covered = registry.covered();
            Set<Endpoint> missing = registry.missing();
            double pct = registry.coveragePercent();

            summaryLabel.setText(String.format(
                    "Total: %d  |  Covered: %d  |  Missing: %d  |  Coverage: %.1f%%",
                    registry.documented().size(),
                    covered.size(), missing.size(), pct));
            progressBar.setValue((int) Math.round(pct));

            missing.stream()
                    .sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .forEach(e -> tableModel.addRow(new Object[]{
                            "\u274C MISSING", e.method(), e.pathTemplate()}));

            covered.stream()
                    .sorted((a, b) -> a.toString().compareTo(b.toString()))
                    .forEach(e -> tableModel.addRow(new Object[]{
                            "\u2705 Covered", e.method(), e.pathTemplate()}));
        });
    }

    public JComponent component() { return panel; }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            String s = String.valueOf(v);
            if (col == 0) {
                c.setForeground(s.startsWith("\u274C")
                        ? new Color(200, 0, 0)
                        : new Color(0, 140, 0));
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else {
                c.setForeground(sel ? t.getSelectionForeground() : t.getForeground());
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
            }
            return c;
        }
    }
}
