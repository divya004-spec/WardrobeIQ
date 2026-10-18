# WardrobeIQ 👗

**Smart Wardrobe Management and Outfit Recommendations**

WardrobeIQ is an Android application designed to help users organize their clothing collection, manage wardrobe items, and get outfit recommendations based on clothing attributes and personal preferences.

## ✨ Features

- **User Authentication:** User registration and login.
- **Digital Wardrobe:** Add and manage clothing items in a personal wardrobe.
- **Clothing Images:** Capture photos using the camera or select images from the gallery.
- **Clothing Categorization:** Organize items by category, color, pattern, occasion, and season.
- **Warmth and Formality Ratings:** Classify clothing based on warmth and formality levels.
- **Image Analysis:** Use image labeling to help identify wardrobe-related items.
- **Local Database:** Store clothing information using Room.
- **Wear Tracking:** Track how often clothing items are worn.
- **Outfit Recommendations:** Support smart outfit selection based on wardrobe information.

## 🛠️ Technology Stack

- **Language:** Java
- **Platform:** Android
- **IDE:** Android Studio
- **Database:** Room Persistence Library
- **Image Analysis:** Google ML Kit Image Labeling
- **Backend:** Node.js and Express
- **File Uploads:** Multer
- **Version Control:** Git and GitHub

## 🏗️ Project Structure

```text
WardrobeIQ/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           ├── res/
│           └── AndroidManifest.xml
├── gradle/
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

## ⚙️ Getting Started

### Prerequisites

- Android Studio
- Android SDK compatible with the project
- JDK version required by the project's Gradle configuration
- Git

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/divya004-spec/WardrobeIQ.git
   ```

2. Open Android Studio.
3. Select **Open** and choose the cloned `WardrobeIQ` folder.
4. Allow Gradle to sync and download the required dependencies.
5. Configure an Android emulator or connect an Android device.
6. Run the application using Android Studio.

## 🖼️ Adding Clothing Items

1. Open the application and log in.
2. Navigate to the clothing-addition screen.
3. Capture a photo or select an image from the gallery.
4. Choose the appropriate category, color, pattern, occasion, season, warmth, and formality.
5. Save the item to your wardrobe.

## 🌐 Backend

The project also includes a separate Node.js and Express backend.

Backend repository: [WardrobeIQBackend](https://github.com/divya004-spec/WardrobeIQBackend)

The backend is intended to support server-side functionality and image upload operations. Backend deployment and integration should be configured according to the current project setup.

## 🚧 Project Status

**Development in progress.**

WardrobeIQ is being developed as a mobile computing mini-project. Features and integrations may change as development continues.

## 🔮 Future Enhancements

- Improved personalized outfit recommendations.
- Weather-aware outfit suggestions.
- Advanced wardrobe analytics and wear statistics.
- Enhanced image-based clothing classification.
- Improved synchronization between the Android app and backend.
- A polished and accessible user interface.

## 👩‍💻 Author

**Divya Digambar Basutkar**

GitHub: [@divya004-spec](https://github.com/divya004-spec)

## 📄 License

No license has been specified yet.
