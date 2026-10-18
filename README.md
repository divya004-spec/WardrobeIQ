# 👗 WardrobeIQ — Smart Wardrobe Management App

WardrobeIQ is an Android application designed to help users organize their clothing digitally and manage wardrobe items in one place. Users can add clothing images, categorize items, and store clothing details for easier wardrobe management.

## ✨ Features

- 📸 **Add Clothing Items:** Select clothing images using the camera or gallery.
- 🤖 **Image Analysis:** Uses Google ML Kit image labeling to analyze selected images.
- 🗂️ **Clothing Categorization:** Organize clothing by category, color, pattern, occasion, season, warmth, and formality.
- 💾 **Local Storage:** Stores clothing information using Room Database.
- 👕 **Digital Wardrobe:** Keep clothing details organized within the application.
- 🔌 **Backend Integration:** A separate Node.js and Express backend is maintained for server-side functionality and image uploads.


## 📱 Screenshots

Added screenshots of your running application to the `screenshots/` folder in this repository.

### Wardrobe Screen

![Wardrobe Screen](screenshots/wardrobe.png)

### Add Clothing Screen

![Add Clothing Screen](screenshots/add-clothing.png)

### Clothing Details / Categorization

![Clothing Details Screen](screenshots/clothing-details.png)

### Image Selection or Analysis

![Image Analysis Screen](screenshots/image-analysis.png)

## 🛠️ Technologies Used

### Android Application
- Java
- Android Studio
- XML layouts
- Room Database
- Google ML Kit Image Labeling
- Android Camera and Gallery APIs

### Backend
- Node.js
- Express.js
- Multer for handling file uploads

**Backend repository:**  
https://github.com/divya004-spec/WardrobeIQBackend

## 🏗️ Project Structure

```text
WardrobeIQ/
├── app/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── README.md
└── screenshots/
    ├── wardrobe.png
    ├── add-clothing.png
    ├── clothing-details.png
    └── image-analysis.png
```

## 🚀 Getting Started

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Allow Gradle to sync and download the required dependencies.
4. Run the application on an Android emulator or a compatible Android device.

## 🔗 Repositories

- **Android Application:** https://github.com/divya004-spec/WardrobeIQ
- **Backend API:** https://github.com/divya004-spec/WardrobeIQBackend

## 🎯 Project Goal

The goal of WardrobeIQ is to make wardrobe organization more convenient through digital clothing management and image-based analysis, with scope for future smart outfit recommendations.

## 👩‍💻 Developed As

An Android development mini-project focused on mobile application development, local data persistence, image analysis, and backend integration.
