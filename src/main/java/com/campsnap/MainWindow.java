package com.campsnap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainWindow extends JFrame {
    private DefaultTableModel tableModel;
    private JTable photoTable;
    private JLabel dateDifferenceLabel;
    private File[] selectedFiles;
    private long timeToAdd;
    private JTextField daysField;
    private ButtonGroup timeModeGroup;
    private JRadioButton autoTimeButton;
    private JRadioButton manualTimeButton;
    private boolean useAutoTime = true;

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
        
        // Top panel with select button, date difference label, and time adjustment controls
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectButton);
        
        // Create time adjustment panel
        JPanel timeAdjustPanel = new JPanel();
        timeAdjustPanel.setBorder(BorderFactory.createTitledBorder("Time Adjustment"));
        
        // Radio buttons for auto/manual mode
        timeModeGroup = new ButtonGroup();
        autoTimeButton = new JRadioButton("Automatic (Based on Most Recent Photo)");
        manualTimeButton = new JRadioButton("Manual Adjustment");
        timeModeGroup.add(autoTimeButton);
        timeModeGroup.add(manualTimeButton);
        autoTimeButton.setSelected(true);
        
        // Manual time adjustment field
        JPanel manualPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        daysField = new JTextField(5);
        daysField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null)
                    return;
                String newValue = getText(0, getLength()) + str;
                try {
                    // Only allow positive integers
                    if (newValue.isEmpty() || Integer.parseInt(newValue) >= 0) {
                        super.insertString(offs, str, a);
                    }
                } catch (NumberFormatException e) {
                    // If not a number, don't insert
                    return;
                }
            }
        });
        
        manualPanel.add(new JLabel("Number of days to add:"));
        manualPanel.add(daysField);
        
        // Layout time adjustment panel
        timeAdjustPanel.setLayout(new BoxLayout(timeAdjustPanel, BoxLayout.Y_AXIS));
        timeAdjustPanel.add(autoTimeButton);
        timeAdjustPanel.add(manualTimeButton);
        timeAdjustPanel.add(manualPanel);
        
        // Add action listeners for radio buttons and text field
        autoTimeButton.addActionListener(e -> {
            boolean isAuto = autoTimeButton.isSelected();
            useAutoTime = isAuto;
            daysField.setEnabled(!isAuto);
            if (selectedFiles != null && selectedFiles.length > 0) {
                processPhotos(selectedFiles);
            }
        });
        
        manualTimeButton.addActionListener(e -> {
            boolean isManual = manualTimeButton.isSelected();
            useAutoTime = !isManual;
            daysField.setEnabled(isManual);
            if (selectedFiles != null && selectedFiles.length > 0) {
                processPhotos(selectedFiles);
            }
        });

        // Add document listener to days field
        daysField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                if (selectedFiles != null && selectedFiles.length > 0 && !useAutoTime) {
                    processPhotos(selectedFiles);
                }
            }
            
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
        });
        
        // Initially disable field since auto mode is default
        daysField.setEnabled(false);
        
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(dateDifferenceLabel, BorderLayout.CENTER);
        topPanel.add(timeAdjustPanel, BorderLayout.SOUTH);
        
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

        if (useAutoTime) {
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
        } else {
            // Calculate manual time difference
            long days = 0;
            try {
                String daysText = daysField.getText().trim();
                if (!daysText.isEmpty()) {
                    days = Long.parseLong(daysText);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, use 0
            }
            
            this.timeToAdd = days * 24L * 60L * 60L * 1000L;
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
                        
                        // Calculate new date by adding the time difference
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

        // Update the date difference label
        if (useAutoTime && mostRecentDate != null) {
            long diffInDays = timeToAdd / (24 * 60 * 60 * 1000);
            String recentDateStr = dateFormat.format(mostRecentDate);
            dateDifferenceLabel.setText(String.format(
                "<html>Most recent photo: %s<br>Days since then: %d</html>",
                recentDateStr, diffInDays
            ));
        } else if (!useAutoTime) {
            String daysText = daysField.getText().trim();
            long days = daysText.isEmpty() ? 0 : Long.parseLong(daysText);
            dateDifferenceLabel.setText(String.format(
                "<html>Manual adjustment:<br>%d days</html>",
                days
            ));
        } else {
            dateDifferenceLabel.setText("No valid dates found in photos");
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

        if (modifyOriginal) {
            // Show confirmation dialog for modifying original files
            int result = JOptionPane.showConfirmDialog(this,
                "This will modify the original photos. This action cannot be undone.\n" +
                "Are you sure you want to continue?",
                "Confirm Modification",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                updateOriginalPhotos();
            }
        } else {
            // Handle "Copy to Folder" option
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Destination Folder");

            if (chooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
                File destinationDir = chooser.getSelectedFile();
                copyPhotosToFolder(destinationDir);
            }
        }
    }

    private void updateOriginalPhotos() {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

        for (File file : selectedFiles) {
            try {
                // Check if file exists and is writable
                if (!file.exists()) {
                    throw new IOException("File does not exist: " + file.getName());
                }
                if (!file.canWrite()) {
                    throw new IOException("Cannot write to file: " + file.getName());
                }

                // Get the original date and calculate new date
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                
                if (directory != null) {
                    Date originalDate = directory.getDateOriginal();
                    if (originalDate != null) {
                        // Calculate new date by adding the time difference
                        Date newDate = new Date(originalDate.getTime() + this.timeToAdd);

                        // Read the image metadata
                        final ImageMetadata imageMetadata = Imaging.getMetadata(file);
                        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
                        final TiffOutputSet outputSet = jpegMetadata != null
                            ? jpegMetadata.getExif().getOutputSet()
                            : new TiffOutputSet();

                        // Update EXIF date fields
                        final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, exifDateFormat.format(newDate));

                        // Create a temporary file for the update
                        File tempFile = File.createTempFile("temp_", "_" + file.getName());
                        
                        // Write the updated metadata to the temporary file
                        try (FileOutputStream fos = new FileOutputStream(tempFile);
                             OutputStream os = new BufferedOutputStream(fos)) {
                            new ExifRewriter().updateExifMetadataLossless(file, os, outputSet);
                        }

                        // Replace the original file with the temporary file
                        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        successCount++;
                    } else {
                        throw new IOException("No original date found in metadata");
                    }
                } else {
                    throw new IOException("No EXIF metadata found");
                }
            } catch (Exception e) {
                errorCount++;
                errorMessages.append(file.getName())
                           .append(": ")
                           .append(e.getMessage())
                           .append("\n");
                e.printStackTrace();
            }
        }

        // Show results
        if (errorCount > 0) {
            String message = String.format("Process completed with some errors.\n" +
                "Successfully updated: %d\n" +
                "Errors: %d\n\n" +
                "Error details:\n%s",
                successCount, errorCount, errorMessages.toString());
            JOptionPane.showMessageDialog(this,
                message,
                "Update Complete with Errors",
                JOptionPane.WARNING_MESSAGE);
        } else {
            String message = String.format("All files updated successfully.\nTotal files updated: %d",
                successCount);
            JOptionPane.showMessageDialog(this,
                message,
                "Update Complete",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void copyPhotosToFolder(File destinationDir) {
        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

        // Create destination directory if it doesn't exist
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                JOptionPane.showMessageDialog(this,
                    "Could not create destination directory: " + destinationDir.getAbsolutePath(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Copy and process each file
        for (File sourceFile : selectedFiles) {
            try {
                // Create copy of the file
                File destFile = new File(destinationDir, sourceFile.getName());
                
                // Check if source file exists and is readable
                if (!sourceFile.exists()) {
                    throw new IOException("Source file does not exist: " + sourceFile.getName());
                }
                if (!sourceFile.canRead()) {
                    throw new IOException("Cannot read source file: " + sourceFile.getName());
                }

                // Get the original date and calculate new date
                Metadata metadata = ImageMetadataReader.readMetadata(sourceFile);
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                
                if (directory != null) {
                    Date originalDate = directory.getDateOriginal();
                    if (originalDate != null) {
                        // Calculate new date by adding the time difference
                        Date newDate = new Date(originalDate.getTime() + this.timeToAdd);

                        // Read the image metadata
                        final ImageMetadata imageMetadata = Imaging.getMetadata(sourceFile);
                        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
                        final TiffOutputSet outputSet = jpegMetadata != null
                            ? jpegMetadata.getExif().getOutputSet()
                            : new TiffOutputSet();

                        // Update EXIF date fields
                        final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, exifDateFormat.format(newDate));
                        
                        // Write the metadata to the new file
                        try (FileOutputStream fos = new FileOutputStream(destFile);
                             OutputStream os = new BufferedOutputStream(fos)) {
                            new ExifRewriter().updateExifMetadataLossless(sourceFile, os, outputSet);
                        }
                    } else {
                        // If no date in metadata, just copy the file
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    // If no EXIF data, just copy the file
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errorMessages.append(sourceFile.getName())
                           .append(": ")
                           .append(e.getMessage())
                           .append("\n");
                e.printStackTrace();
            }
        }

        // Show results
        if (errorCount > 0) {
            String message = String.format("Process completed with some errors.\n" +
                "Successfully copied and updated: %d\n" +
                "Errors: %d\n\n" +
                "Error details:\n%s",
                successCount, errorCount, errorMessages.toString());
            JOptionPane.showMessageDialog(this,
                message,
                "Copy Complete with Errors",
                JOptionPane.WARNING_MESSAGE);
        } else {
            String message = String.format("All files copied and metadata updated successfully.\nTotal files processed: %d",
                successCount);
            JOptionPane.showMessageDialog(this,
                message,
                "Copy Complete",
                JOptionPane.INFORMATION_MESSAGE);
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