# CampSnap Time Taken Updater

This application helps you update the "Date Taken" metadata of your photos, particularly useful for photos taken with cameras that have incorrect dates set.

## Requirements

- Java 17 or later installed on your computer

## Installation

1. Download and extract the ZIP file to any location on your computer
2. Make sure you have Java installed (see Requirements above)

## Running the Application

You can either run the JAR file included or you can compile it yourself following the steps below:

### Windows:
- Double-click `run.bat`
- Or open Command Prompt, navigate to the folder, and run `run.bat`

### macOS and Linux:
1. Open Terminal
2. Navigate to the folder where you extracted the files
3. Make the script executable (first time only):
   ```bash
   chmod +x run.sh
   ```
4. Run the application:
   ```bash
   ./run.sh
   ```

## Using the Application

1. Click "Select Photos" to choose the JPEG photos you want to modify
2. The table will show current dates and calculated new dates
3. Choose your time adjustment method:
   - "Automatic (Based on Most Recent Photo)": Automatically calculates the time difference based on the most recent photo
   - "Manual Adjustment": Enter the number of days you want to add to the photos' dates
4. Choose either:
   - "Modify Original Photo" to update the original files
   - "Copy to Folder" to create copies with updated dates
5. Click "Update Dates" to process the photos

### Time Adjustment Options

#### Automatic Mode
- The app finds the most recent photo in your selection
- Calculates the difference between that photo's date and today
- Applies this difference to all selected photos

#### Manual Mode
- Enter the number of days you want to add to the photos
- The preview table updates automatically as you type
- All selected photos will have their dates moved forward by the specified number of days

## Important Notes

- Always keep backups of your important photos
- The application only works with JPEG images that contain EXIF metadata
- When using "Modify Original Photo", the original files will be changed
- The manual adjustment only accepts positive whole numbers (no negative days or decimal values)
- Changes to dates are permanent when using "Modify Original Photo"

## Tips
- Use "Copy to Folder" first to test your changes before modifying original photos
- Review the preview table to confirm the new dates before proceeding
- If you're unsure about the time difference, use manual mode for precise control

## Support

If you encounter any issues, please report them on the GitHub repository:
[https://github.com/Swordguy98/CampSnapTimeTakenUpdater/issues](https://github.com/Swordguy98/CampSnapTimeTakenUpdater/issues)
