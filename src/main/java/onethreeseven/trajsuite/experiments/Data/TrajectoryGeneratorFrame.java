package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.datastructures.algorithm.TrajectoryDragonCurve;

import javax.swing.*;
import java.awt.*;

/**
 * A swing experiment for generating trajectories.
 * @author Luke Bermingham
 */
public abstract class TrajectoryGeneratorFrame extends JInternalFrame {
    private final boolean realTimeUpdates;
    private JPanel contentPanel;
    private JSpinner complexitySpinner;
    private JSlider straightnessSlider;
    private JLabel straightnessLabel;
    private JSlider volatilitySlider;
    private JLabel volatilityLabel;
    private JButton newSeedButton;
    private JButton generateButton;
    private JSpinner nTrajectoriesSpinner;
    private JSpinner limitSpinner;

    public TrajectoryGeneratorFrame(TrajectoryDragonCurve parameters, boolean realTimeUpdates) {
        super("Trajectory Generator", true, true, false, true);
        this.realTimeUpdates = realTimeUpdates;
        setupUi();
        setContentPane(contentPanel);
        this.pack();
        this.setVisible(true);
        volatilitySlider.setValue((int) (parameters.getVolatility() * 100));
        straightnessSlider.setValue((int) (parameters.getStraightness() * 100));
        complexitySpinner.setValue(parameters.getComplexity());
        limitSpinner.setValue(parameters.getLimit());
    }

    private void createUIComponents() {

        volatilitySlider = new JSlider(0, 100, 0);
        straightnessSlider = new JSlider(0, 100, 0);
        complexitySpinner = new JSpinner(new SpinnerNumberModel(15, 1, 100, 1));
        newSeedButton = new JButton();
        nTrajectoriesSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1));
        generateButton = new JButton();
        limitSpinner = new JSpinner(new SpinnerNumberModel(10000, 3, 3000000, 1));

        generateButton.addActionListener(e -> fireUpdateTrajectory(false));

        newSeedButton.addActionListener(e -> fireUpdateTrajectory(true));

        volatilitySlider.addChangeListener(e -> {
            volatilityLabel.setText(String.valueOf(volatilitySlider.getValue()) + "%");
            if (realTimeUpdates) {
                fireUpdateTrajectory(false);
            }

        });

        straightnessSlider.addChangeListener(e -> {
            straightnessLabel.setText(String.valueOf(straightnessSlider.getValue()) + "%");
            if (realTimeUpdates) {
                fireUpdateTrajectory(false);
            }

        });

        nTrajectoriesSpinner.addChangeListener(e -> {
            if (realTimeUpdates) {
                fireUpdateTrajectory(false);
            }

        });

        complexitySpinner.addChangeListener(e -> {
            if (realTimeUpdates) {
                fireUpdateTrajectory(false);
            }

        });

        limitSpinner.addChangeListener(e -> {
            if (realTimeUpdates) {
                fireUpdateTrajectory(false);
            }
        });

    }

    private void fireUpdateTrajectory(boolean newSeed) {
        double volatility = volatilitySlider.getValue() / 100.0;
        double straightness = straightnessSlider.getValue() / 100.0;
        Integer complexity = (Integer) complexitySpinner.getValue();
        Integer nTrajectories = (Integer) nTrajectoriesSpinner.getValue();
        Integer limit = (Integer) limitSpinner.getValue();

        TrajectoryDragonCurve algo = new TrajectoryDragonCurve();
        algo.setVolatility(volatility);
        algo.setComplexity(complexity);
        algo.setStraightness(straightness);
        algo.setLimit(limit);

        updateTrajectory(algo, nTrajectories, newSeed);
    }

    public abstract void updateTrajectory(TrajectoryDragonCurve algo, int nTrajectories, boolean newSeed);

    private void setupUi() {
        createUIComponents();
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        final JPanel spacer1 = new JPanel();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        contentPanel.add(spacer1, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Straightness: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label1, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Volatility: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label2, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("Complexity: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label3, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(complexitySpinner, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(straightnessSlider, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(volatilitySlider, gbc);
        final JLabel label4 = new JLabel();
        label4.setFont(new Font(label4.getFont().getName(), Font.BOLD, 16));
        label4.setText("Trajectory Generator Settings");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label4, gbc);
        newSeedButton.setText("New Seed");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(newSeedButton, gbc);
        generateButton.setText("Generate");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(generateButton, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.VERTICAL;
        contentPanel.add(spacer2, gbc);
        final JLabel label5 = new JLabel();
        label5.setText("N Trajectories: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label5, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(nTrajectoriesSpinner, gbc);
        final JLabel label6 = new JLabel();
        label6.setText("Longitude Range: (-180, 180)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label6, gbc);
        final JLabel label7 = new JLabel();
        label7.setText("Latitude Range: (-90, 90)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label7, gbc);
        final JLabel label8 = new JLabel();
        label8.setText(" to: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label8, gbc);
        final JLabel label9 = new JLabel();
        label9.setText(" to: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label9, gbc);
        straightnessLabel = new JLabel();
        straightnessLabel.setText("0%");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(straightnessLabel, gbc);
        volatilityLabel = new JLabel();
        volatilityLabel.setText("0%");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(volatilityLabel, gbc);
        final JLabel label10 = new JLabel();
        label10.setText("Limit: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(label10, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(limitSpinner, gbc);
    }

}
