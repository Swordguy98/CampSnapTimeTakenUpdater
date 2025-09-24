# CampSnap Time Taken Updater

This application helps you update the "Date Taken" metadata of your photos, particularly useful for photos taken with cameras that have incorrect dates set.

## Requirements

- Java 17 or later installed on your computer
  - Windows: Download from [Eclipse Adoptium](https://adoptium.net/)
  - macOS: Download from [Eclipse Adoptium](https://adoptium.net/)
  - Linux: Download from [Eclipse Adoptium](https://adoptium.net/) or use your package manager

## Installation

1. Download and extract the ZIP file to any location on your computer
2. Make sure you have Java installed (see Requirements above)

## Running the Application

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
3. Choose either:
   - "Modify Original Photo" to update the original files
   - "Copy to Folder" to create copies with updated dates
4. Click "Update Dates" to process the photos

## Important Notes

- Always keep backups of your important photos
- The application only works with JPEG images that contain EXIF metadata
- When using "Modify Original Photo", the original files will be changed

## Support

If you encounter any issues, please report them on the GitHub repository:
[https://github.com/Swordguy98/CampSnapTimeTakenUpdater/issues](https://github.com/Swordguy98/CampSnapTimeTakenUpdater/issues)