# B&B Luxury Cars - Car Rental Application

A sophisticated Android application for renting premium and standard vehicles with an elegant user interface, comprehensive booking management system, and seamless user experience.

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Screenshots](#screenshots)
- [Technology Stack](#technology-stack)
- [Setup Instructions](#setup-instructions)
- [Project Structure](#project-structure)
- [Usage Guide](#usage-guide)
- [API Documentation](#api-documentation)
- [Development Roadmap](#development-roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Contributors](#contributors)

## Overview

B&B Luxury Cars is a feature-rich mobile application designed to revolutionize the car rental experience. The application provides a seamless journey from user registration to booking confirmation, with comprehensive features for managing rental history, user profiles, and vehicle preferences.

The platform caters to both everyday users seeking standard vehicles and luxury car enthusiasts looking for premium driving experiences. With an intuitive interface and robust backend, B&B Luxury Cars streamlines the entire rental process while maintaining high security standards for user data and transactions.

## Key Features

### User Management
- **Secure Authentication**: Email/password login with Firebase Authentication
- **User Registration**: Comprehensive sign-up process with driver's license verification
- **Profile Management**: Update personal information, contact details, and driver credentials
- **Session Handling**: Persistent login sessions with secure token management

### Vehicle Management
- **Categorized Browsing**: Filter vehicles by type (SUV, Compact, Sport Car, etc.)
- **Advanced Search**: Find specific vehicles by model, make, or features
- **Detailed Vehicle Information**: Comprehensive specs, high-quality images, and availability status
- **Price Transparency**: Clear daily rental rates with no hidden fees

### Booking System
- **Date Selection**: Intuitive calendar interface for rental period selection
- **Special Requests**: Add custom requirements for vehicle preparation
- **Booking Status Tracking**: Monitor pending, confirmed, and completed rentals
- **Rental History**: Complete log of past and current rentals with details

### Admin Features
- **Inventory Management**: Add, update, or remove vehicles from the fleet
- **Booking Administration**: Review, approve, or reject rental requests
- **User Management**: Monitor user activities and manage accounts
- **Analytics Dashboard**: Track rental patterns and popular vehicle choices

### User Experience
- **Responsive Design**: Optimized for various Android device sizes
- **Offline Capability**: Basic functionality available without internet connection
- **Push Notifications**: Updates on booking status and special offers
- **Multilingual Support**: Interface available in multiple languages (planned)

## Screenshots

### Authentication Screens

#### Login Screen
![Login Screen](/screenshots/login_screen.jpg)
*The login interface features the B&B Luxury Cars branding with secure email and password authentication, along with options for password recovery and new account registration.*

#### Registration Screen
![Registration Screen](/screenshots/registration_screen.jpg)
*The registration form collects essential user information including full name, email, phone number, driver's license details, and password with visibility toggle for enhanced security.*

### Main Application Screens

#### Home Screen - Vehicle Browsing
![Home Screen](/screenshots/home_screen.jpg)
*The home screen showcases available vehicles with category filtering options (ALL, SUV, COMPACT), search functionality, and a clean card-based layout displaying high-quality vehicle images.*

#### Vehicle Details
![Vehicle Details](/screenshots/vehicle_details.jpg)
*Detailed vehicle information page displaying the Toyota Camry with sedan classification, daily rental rate of $459.99, and availability status with a prominent "Disponible" (Available) button.*

#### User Profile
![User Profile](/screenshots/user_profile.jpg)
*The user profile section displays personal information including name, email, phone number, and driver's license details, with a logout option at the bottom of the screen.*

#### Rental History
![Rental History](/screenshots/rental_history.jpg)
*The rental history page shows past and current bookings with vehicle details, rental dates, status indicators, and any special requests made during booking.*

## Technology Stack

### Frontend
- **Language**: Java
- **Platform**: Android SDK
- **Minimum SDK Version**: API 21 (Android 5.0 Lollipop)
- **Target SDK Version**: API 33 (Android 13)
- **UI Framework**: Android Jetpack Components
- **UI Design**: Material Design Components
- **Image Loading**: Glide/Picasso
- **Animations**: MotionLayout, Property Animations

### Backend
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Authentication
- **Storage**: Firebase Cloud Storage (for vehicle images)
- **Analytics**: Firebase Analytics
- **Notifications**: Firebase Cloud Messaging

### Development Tools
- **IDE**: Android Studio
- **Build System**: Gradle (Kotlin DSL)
- **Version Control**: Git
- **CI/CD**: GitHub Actions (planned)
- **Code Quality**: Lint, SonarQube (planned)

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **Components**: LiveData, ViewModel, Repository
- **Dependency Injection**: Hilt/Dagger (planned)
- **Asynchronous Operations**: Coroutines/RxJava

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11 or higher
- Android SDK with API level 33
- Firebase account

### Installation Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/MohamedMBG/CarRentingTest.git
   cd CarRentingTest
   ```

2. Open the project in Android Studio:
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository and select it

3. Sync Gradle files:
   - Android Studio should automatically sync the Gradle files
   - If not, select "Sync Project with Gradle Files" from the toolbar

4. Configure Firebase:
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app to your Firebase project:
     - Use package name: `com.example.carrentingtest`
     - Download the `google-services.json` file
     - Place the file in the app directory
   - Enable Authentication in Firebase console:
     - Set up Email/Password sign-in method
   - Create Realtime Database:
     - Start in test mode for development
     - Set up security rules for production

5. Build and run the application:
   - Select a target device (emulator or physical device)
   - Click the "Run" button in Android Studio

### Troubleshooting Common Issues

- **Build Failures**: Ensure Gradle version compatibility and update Android Studio if needed
- **Firebase Connection Issues**: Verify `google-services.json` is correctly placed and Firebase services are enabled
- **Emulator Problems**: Update Android Emulator and HAXM for better performance

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/carrentingtest/
│   │   │   ├── adapters/       # RecyclerView adapters for lists
│   │   │   ├── models/         # Data models and entities
│   │   │   ├── repositories/   # Data access layer
│   │   │   ├── ui/             # Activities, Fragments, ViewModels
│   │   │   │   ├── auth/       # Authentication screens
│   │   │   │   ├── home/       # Main screens and navigation
│   │   │   │   ├── booking/    # Booking flow screens
│   │   │   │   ├── profile/    # User profile management
│   │   │   │   └── admin/      # Admin-specific screens
│   │   │   ├── utils/          # Utility classes and helpers
│   │   │   ├── services/       # Background services
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   ├── layout/         # XML layout files
│   │   │   ├── drawable/       # Images and drawable resources
│   │   │   ├── values/         # Strings, colors, styles
│   │   │   ├── navigation/     # Navigation graphs
│   │   │   └── anim/           # Animation resources
│   │   └── AndroidManifest.xml
│   ├── androidTest/            # Instrumentation tests
│   └── test/                   # Unit tests
├── build.gradle.kts            # App-level build configuration
└── proguard-rules.pro          # ProGuard rules for optimization
```

## Usage Guide

### For Users

#### Creating an Account
1. Launch the B&B Luxury Cars application
2. Tap "Sign Up" on the login screen
3. Fill in your personal details, including:
   - Full name
   - Email address
   - Phone number
   - Driver's license number
   - Secure password
4. Tap "SIGN UP" to create your account
5. Verify your email if prompted

#### Browsing and Renting Vehicles
1. Use the category tabs (ALL, SUV, COMPACT) to filter vehicles
2. Scroll through available vehicles
3. Tap on a vehicle card to view detailed information
4. Check daily rental rate and availability
5. Tap "Disponible" (Available) to proceed with booking
6. Select rental dates from the calendar
7. Add any special requests if needed
8. Confirm booking details and submit

#### Managing Your Profile
1. Navigate to the Profile tab from the bottom navigation bar
2. View your personal information
3. Update details as needed
4. Use the LOGOUT button to sign out securely

#### Viewing Rental History
1. Tap the History tab in the bottom navigation
2. View all past and current rentals
3. Check status (pending, confirmed, completed)
4. Review special requests and rental dates

### For Administrators

#### Accessing Admin Panel
1. Log in with admin credentials
2. Tap "Admin" on the login screen
3. Enter admin-specific password if prompted

#### Managing Vehicle Inventory
1. Navigate to the Admin Dashboard
2. Select "Manage Vehicles"
3. Add new vehicles with details and images
4. Edit existing vehicle information
5. Mark vehicles as unavailable for maintenance

#### Processing Booking Requests
1. View incoming booking requests
2. Review customer details and rental dates
3. Approve or reject bookings
4. Add notes for internal reference

## API Documentation

### Firebase Authentication

```java
// User registration example
mAuth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            // User registration successful
            FirebaseUser user = mAuth.getCurrentUser();
            // Save additional user data to database
            saveUserDataToDatabase(user.getUid(), name, phone, licenseNumber);
        } else {
            // Handle registration failure
        }
    });
```

### Firebase Realtime Database

```java
// Save user data example
private void saveUserDataToDatabase(String userId, String name, String phone, String license) {
    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
    
    Map<String, Object> userData = new HashMap<>();
    userData.put("name", name);
    userData.put("phone", phone);
    userData.put("licenseNumber", license);
    userData.put("createdAt", ServerValue.TIMESTAMP);
    
    userRef.setValue(userData)
        .addOnSuccessListener(aVoid -> {
            // Data saved successfully
        })
        .addOnFailureListener(e -> {
            // Handle failure
        });
}
```

### Vehicle Data Structure

```json
{
  "vehicles": {
    "vehicle_id_1": {
      "model": "Porsche 911",
      "year": "2025",
      "category": "Sport Car",
      "dailyRate": 1000.00,
      "available": true,
      "imageUrl": "https://firebasestorage.googleapis.com/...",
      "features": ["Leather Seats", "GPS Navigation", "Sport Mode"]
    },
    "vehicle_id_2": {
      "model": "Toyota Camry",
      "year": "2024",
      "category": "Sedan",
      "dailyRate": 459.99,
      "available": true,
      "imageUrl": "https://firebasestorage.googleapis.com/...",
      "features": ["Bluetooth", "Backup Camera", "Cruise Control"]
    }
  }
}
```

## Development Roadmap

### Short-term Goals (Next 3 Months)
- Implement payment gateway integration
- Add vehicle ratings and reviews
- Enhance search with filters for price range and features
- Implement push notifications for booking updates

### Mid-term Goals (6-12 Months)
- Add multilingual support (English, French, Arabic)
- Develop iOS version of the application
- Implement loyalty program for frequent customers
- Add vehicle location tracking with maps integration

### Long-term Vision
- Expand to multiple regions with localized vehicle offerings
- Implement AI-based recommendation system
- Develop contactless vehicle pickup system
- Create a cross-platform web interface

## Contributing

We welcome contributions to improve B&B Luxury Cars! Here's how you can help:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add some amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Coding Standards
- Follow Java code style guidelines
- Write unit tests for new features
- Update documentation for API changes
- Ensure UI consistency with Material Design principles

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributors

- [MohamedMBG (BAGHDAD Mohamed)](https://github.com/MohamedMBG) - Project Lead & Backend Developer
- [monssefbaakka (Monssef baakka)](https://github.com/monssefbaakka) - UI/UX Designer & Frontend Developer
