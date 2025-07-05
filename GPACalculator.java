import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;

public class GPACalculator extends JFrame {
    private JPanel inputPanel;
    private JLabel gpaLabel, classificationLabel;
    private java.util.List<SubjectRow> subjectRows = new ArrayList<>();
    private Connection connection;

    public GPACalculator() {
        setTitle("GPA Calculator - Dark Mode");
        setSize(700, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(40, 40, 40));
        connectDB();

        JLabel title = new JLabel("GPA Calculator", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(20, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(new Color(40, 40, 40));
        JScrollPane scrollPane = new JScrollPane(inputPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        add(scrollPane, BorderLayout.CENTER);

        addSubjectRow();

        JButton addRowBtn = createButton("+ Add Subject");
        addRowBtn.addActionListener(e -> addSubjectRow());

        JButton calcBtn = createButton("Calculate GPA");
        calcBtn.addActionListener(e -> calculateGPA());

        JButton viewHistoryBtn = createButton("View Past Grades");
        viewHistoryBtn.addActionListener(e -> showPastGrades());

        gpaLabel = new JLabel("GPA: --", SwingConstants.CENTER);
        gpaLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        gpaLabel.setForeground(Color.GREEN);

        classificationLabel = new JLabel("", SwingConstants.CENTER);
        classificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        classificationLabel.setForeground(Color.ORANGE);

        JPanel bottomPanel = new JPanel(new GridLayout(5, 1));
        bottomPanel.setBackground(new Color(40, 40, 40));
        bottomPanel.add(addRowBtn);
        bottomPanel.add(calcBtn);
        bottomPanel.add(viewHistoryBtn);
        bottomPanel.add(gpaLabel);
        bottomPanel.add(classificationLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void connectDB() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/gpa_app", "root", "@wennuan_26");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed!");
            e.printStackTrace();
        }
    }

    private void addSubjectRow() {
        SubjectRow row = new SubjectRow();
        subjectRows.add(row);
        inputPanel.add(row);
        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private void calculateGPA() {
        float totalPoints = 0;
        float totalCredits = 0;

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO grades (subject, credit, grade, point) VALUES (?, ?, ?, ?)")) {

            for (SubjectRow row : subjectRows) {
                float credit = Float.parseFloat(row.creditField.getText());
                String grade = row.gradeBox.getSelectedItem().toString();
                float point = getGradePoint(grade);
                totalPoints += point * credit;
                totalCredits += credit;

                stmt.setString(1, row.subjectField.getText());
                stmt.setFloat(2, credit);
                stmt.setString(3, grade);
                stmt.setFloat(4, point);
                stmt.addBatch();
            }

            stmt.executeBatch();

            float gpa = totalPoints / totalCredits;
            gpaLabel.setText(String.format("GPA: %.2f", gpa));
            showClassification(gpa);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: Please enter valid credit numbers and ensure all fields are filled.");
        }
    }

    private void showClassification(float gpa) {
        if (gpa >= 3.7f) {
            classificationLabel.setText("You're on track for a First Class degree 🎉");
        } else if (gpa >= 3.3f) {
            classificationLabel.setText(String.format("You're on track for a Second Class (Upper) degree. Need %.2f more GPA for First Class.", 3.7f - gpa));
        } else {
            classificationLabel.setText(String.format("You need %.2f more GPA for Second Class.", 3.3f - gpa));
        }
    }

    private void showPastGrades() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT subject, credit, grade, point FROM grades");

            JTextArea area = new JTextArea(15, 50);
            area.setFont(new Font("Monospaced", Font.PLAIN, 14));
            area.setBackground(Color.BLACK);
            area.setForeground(Color.GREEN);
            area.append(String.format("%-20s %-10s %-10s %-10s%n", "Subject", "Credit", "Grade", "Points"));
            area.append("----------------------------------------------------------\n");

            while (rs.next()) {
                area.append(String.format("%-20s %-10.1f %-10s %-10.1f%n",
                        rs.getString("subject"),
                        rs.getFloat("credit"),
                        rs.getString("grade"),
                        rs.getFloat("point")));
            }

            JOptionPane.showMessageDialog(this, new JScrollPane(area), "Past Grades", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to fetch past grades.");
        }
    }

    private float getGradePoint(String grade) {
        return switch (grade.toUpperCase()) {
            case "A+", "A" -> 4.0f;
            case "A-" -> 3.7f;
            case "B+" -> 3.3f;
            case "B" -> 3.0f;
            case "B-" -> 2.7f;
            case "C+" -> 2.3f;
            case "C" -> 2.0f;
            case "C-" -> 1.7f;
            case "D" -> 1.0f;
            case "F" -> 0.0f;
            default -> 0.0f;
        };
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(60, 60, 60));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 10, 10, 10));
        return btn;
    }

    class SubjectRow extends JPanel {
        JTextField subjectField;
        JTextField creditField;
        JComboBox<String> gradeBox;

        SubjectRow() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
            setBackground(new Color(40, 40, 40));

            subjectField = new JTextField(10);
            creditField = new JTextField(4);
            gradeBox = new JComboBox<>(new String[]{
                "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "F"
            });

            stylizeField(subjectField);
            stylizeField(creditField);
            stylizeComboBox(gradeBox);

            JLabel subLbl = new JLabel("Subject:");
            JLabel credLbl = new JLabel("Credits:");
            JLabel gradLbl = new JLabel("Grade:");
            subLbl.setForeground(Color.WHITE);
            credLbl.setForeground(Color.WHITE);
            gradLbl.setForeground(Color.WHITE);

            gradeBox.addActionListener(e -> {
                String selectedGrade = gradeBox.getSelectedItem().toString();
                float point = getGradePoint(selectedGrade);

                if (creditField.getText().isEmpty() || isDefaultCredit(creditField.getText())) {
                    if (point >= 3.7) creditField.setText("4.0");
                    else if (point >= 3.0) creditField.setText("3.0");
                    else if (point >= 2.0) creditField.setText("2.0");
                    else creditField.setText("1.0");
                }
            });

            add(subLbl);
            add(subjectField);
            add(credLbl);
            add(creditField);
            add(gradLbl);
            add(gradeBox);
        }

        private boolean isDefaultCredit(String text) {
            return text.equals("4.0") || text.equals("3.0") || text.equals("2.0") || text.equals("1.0");
        }

        private void stylizeField(JTextField field) {
            field.setBackground(new Color(60, 60, 60));
            field.setForeground(Color.WHITE);
            field.setCaretColor(Color.WHITE);
            field.setFont(new Font("SansSerif", Font.PLAIN, 14));
            field.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        private void stylizeComboBox(JComboBox<String> comboBox) {
            comboBox.setBackground(new Color(60, 60, 60));
            comboBox.setForeground(Color.WHITE);
            comboBox.setFont(new Font("SansSerif", Font.BOLD, 14));
            comboBox.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setBackground(new Color(60, 60, 60));
                    label.setForeground(Color.WHITE);
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    return label;
                }
            });
        }

        private float getGradePoint(String grade) {
            return switch (grade.toUpperCase()) {
                case "A+", "A" -> 4.0f;
                case "A-" -> 3.7f;
                case "B+" -> 3.3f;
                case "B" -> 3.0f;
                case "B-" -> 2.7f;
                case "C+" -> 2.3f;
                case "C" -> 2.0f;
                case "C-" -> 1.7f;
                case "D" -> 1.0f;
                case "F" -> 0.0f;
                default -> 0.0f;
            };
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GPACalculator().setVisible(true));
    }
}
