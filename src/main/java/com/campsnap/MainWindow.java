package com.campsnap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class MainWindow extends JFrame {
    private DefaultTableModel tableModel;
    private JTable photoTable;
    private JLabel dateDifferenceLabel;
    private File[] selectedFiles;
    private long timeToAdd;

    public MainWindow() {
        // Set up the window
        setTitle("CampSnap Time Taken Updater");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen

        // Create the button
        JButton selectButton = new JButton("Select Photos");
        selectButton.setPreferredSize(new Dimension(120, 30));
        selectButton.addActionListener(e -> selectPhotos());

        // Create the date difference label
        dateDifferenceLabel = new JLabel("No photos selected");
        dateDifferenceLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        dateDifferenceLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Create the table
        String[] columnNames = {"File Name", "Date Taken", "New Date"};
        tableModel = new DefaultTableModel(columnNames, 0);
        photoTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(photoTable);

        // Create layout
        setLayout(new BorderLayout());
        
        // Top panel with select button and date difference label
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(dateDifferenceLabel, BorderLayout.CENTER);
        
        // Bottom panel with update options
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        ButtonGroup updateOptionGroup = new ButtonGroup();
        JRadioButton modifyOriginalButton = new JRadioButton("Modify Original Photo");
        JRadioButton copyToFolderButton = new JRadioButton("Copy to Folder");
        updateOptionGroup.add(modifyOriginalButton);
        updateOptionGroup.add(copyToFolderButton);
        copyToFolderButton.setSelected(true); // Default option
        
        JButton updateButton = new JButton("Update Dates");
        updateButton.addActionListener(e -> handleUpdateDates(modifyOriginalButton.isSelected()));
        
        bottomPanel.add(modifyOriginalButton);
        bottomPanel.add(copyToFolderButton);
        bottomPanel.add(updateButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void selectPhotos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".jpeg");
            }

            public String getDescription() {
                return "JPEG Images (*.jpg, *.jpeg)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            processPhotos(files);
        }
    }

    private void processPhotos(File[] files) {
        // Store the selected files for later use
        this.selectedFiles = files;
        
        // Clear existing table data
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date mostRecentDate = null;
        Date currentDate = new Date();

        // First pass: find the most recent date
        for (File file : files) {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                
                if (directory != null) {
                    Date date = directory.getDateOriginal();
                    if (date != null && (mostRecentDate == null || date.after(mostRecentDate))) {
                        mostRecentDate = date;
                    }
                }
            } catch (ImageProcessingException | IOException e) {
                e.printStackTrace();
            }
        }

        // Calculate the time difference to add
        this.timeToAdd = 0;
        if (mostRecentDate != null) {
            this.timeToAdd = currentDate.getTime() - mostRecentDate.getTime();
        }

        // Second pass: process each photo
        for (File file : files) {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                
                String fileName = file.getName();
                String dateTaken = "Unknown";
                String newDate = "Unknown";
                
                if (directory != null) {
                    Date date = directory.getDateOriginal();
                    if (date != null) {
                        dateTaken = dateFormat.format(date);
                        
                        // Calculate new date by adding the same time difference
                        Date calculatedDate = new Date(date.getTime() + this.timeToAdd);
                        newDate = dateFormat.format(calculatedDate);
                    }
                }
                
                tableModel.addRow(new Object[]{fileName, dateTaken, newDate});
            } catch (ImageProcessingException | IOException e) {
                e.printStackTrace();
                tableModel.addRow(new Object[]{file.getName(), "Error reading metadata", "Error"});
            }
        }

        // Calculate and display the date difference
        if (mostRecentDate != null) {
            long diffInMillies = currentDate.getTime() - mostRecentDate.getTime();
            long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);
            
            String recentDateStr = dateFormat.format(mostRecentDate);
            dateDifferenceLabel.setText(String.format(
                "<html>Most recent photo: %s<br>Days since then: %d</html>",
                recentDateStr, diffInDays
            ));
        } else {
            dateDifferenceLabel.setText("No valid dates found in photos");
        }
    }

    private void handleUpdateDates(boolean modifyOriginal) {
        if (selectedFiles == null || selectedFiles.length == 0) {
            JOptionPane.showMessageDialog(this,
                "Please select photos first.",
                "No Photos Selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!modifyOriginal) {
            // Handle "Copy to Folder" option
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Destination Folder");

            if (chooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
                File destinationDir = chooser.getSelectedFile();
                copyPhotosToFolder(destinationDir);
            }
        } else {
            // To be implemented later: Modify original photos
            JOptionPane.showMessageDialog(this,
                "Modify original photos feature will be implemented soon.",
                "Not Implemented",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void copyPhotosToFolder(File destinationDir) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int successCount = 0;
        int errorCount = 0;

        try {
            for (File sourceFile : selectedFiles) {
                try {
                    // Read metadata
                    Metadata metadata = ImageMetadataReader.readMetadata(sourceFile);
                    ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    
                    if (directory != null) {
                        Date originalDate = directory.getDateOriginal();
                        if (originalDate != null) {
                            // Create copy of the file
                            File destFile = new File(destinationDir, sourceFile.getName());
                            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorCount++;
                }
            }

            String message = String.format("Process completed.\nSuccessfully copied: %d\nErrors: %d",
                successCount, errorCount);
            JOptionPane.showMessageDialog(this, message, "Copy Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "An error occurred while copying files: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Create and show the window on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}