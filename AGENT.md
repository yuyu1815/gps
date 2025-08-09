# Android Indoor Positioning Application Development Guidelines

This document provides essential information for developers working on this Android indoor positioning application project that uses BLE beacons and smartphone sensors for high-precision location tracking in indoor environments where GPS is unavailable.

## Build/Configuration Instructions

### Prerequisites
- Android Studio Flamingo (2023.2.1) or newer
- JDK 11 or newer
- Gradle 8.0 or newer
- Android SDK with API level 35 (compileSdk)
- Minimum supported Android version: API level 30 (minSdk)
- Device with BLE (Bluetooth Low Energy) support
- Device with accelerometer, gyroscope, and magnetometer sensors for full functionality

### Project Setup
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Ensure you have the required SDK versions installed through the SDK Manager

### Build Configuration
- The project uses Kotlin DSL for Gradle build scripts
- Jetpack Compose is enabled for UI development
- The application uses Material3 design components
- Edge-to-edge display is enabled by default

### Running the Application
- Connect an Android device or use an emulator with API level 30+
- Run the application using Android Studio's run button or via Gradle:
  ```
  gradlew installDebug
  ```

## Testing Information

### Test Structure
- Unit tests: Located in `app/src/test/java/com/example/myapplication/`
- Instrumented tests: Located in `app/src/androidTest/java/com/example/myapplication/`

### Running Tests
- Unit tests can be run using:
  ```
  gradlew test
  ```
- Instrumented tests can be run using:
  ```
  gradlew connectedAndroidTest
  ```
- To run specific tests:
  ```
  gradlew test --tests "com.example.myapplication.TestClassName"
  ```

### Adding New Tests
- Unit tests:
  1. Create a new Kotlin file in `app/src/test/java/com/example/myapplication/`
  2. Name the file with the pattern `*Test.kt`
  3. Use JUnit 4 annotations (`@Test`, etc.)
  4. Example:
     ```kotlin
     class StringUtilsTest {
         @Test
         fun stringReverse_isCorrect() {
             assertEquals("olleh", reverseString("hello"))
             assertEquals("", reverseString(""))
             assertEquals("a", reverseString("a"))
         }
         
         private fun reverseString(input: String): String {
             return input.reversed()
         }
     }
     ```

- Instrumented tests:
  1. Create a new Kotlin file in `app/src/androidTest/java/com/example/myapplication/`
  2. Name the file with the pattern `*Test.kt`
  3. Use `@RunWith(AndroidJUnit4::class)` annotation
  4. Example:
     ```kotlin
     @RunWith(AndroidJUnit4::class)
     class ExampleInstrumentedTest {
         @Test
         fun useAppContext() {
             val appContext = InstrumentationRegistry.getInstrumentation().targetContext
             assertEquals("com.example.myapplication", appContext.packageName)
         }
     }
     ```

### UI Testing with Compose
- For Compose UI testing, use the `createComposeRule()` or `createAndroidComposeRule()`
- Example:
  ```kotlin
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun myComposeTest() {
      composeTestRule.setContent {
          MyApplicationTheme {
              Greeting("Test")
          }
      }
      
      composeTestRule.onNodeWithText("Hello Test!").assertIsDisplayed()
  }
  ```

## BLE and Sensor Implementation

### BLE Beacon Scanning
- Implement periodic scanning for BLE beacons using `BluetoothLeScanner`
- Record MAC address, RSSI value, and timestamp for each beacon
- Implement beacon staleness management (consider beacons invalid after 5+ seconds without updates)
- Filter RSSI values using moving average or other smoothing techniques
- Calculate distance from RSSI using appropriate propagation models

### Sensor Fusion
- Implement Pedestrian Dead Reckoning (PDR) using device sensors
- Use accelerometer for step detection with peak-valley pattern recognition
- Implement heading estimation primarily using gyroscope data with complementary filter
- Dynamically estimate step length based on walking patterns
- Fuse BLE positioning with PDR using weighted averaging or Kalman filter
- Consider device orientation and handle different carrying positions

### Indoor Mapping
- Load indoor maps and beacon configurations from external JSON files
- Implement coordinate transformation between physical and screen coordinates
- Display user position with uncertainty visualization (semi-transparent circle)
- Implement map zooming and panning functionality

### Calibration and Debugging
- Implement TxPower calibration for beacons (measuring RSSI at 1m distance)
- Adjust environmental factor N based on field measurements
- Create debug overlay to display real-time sensor and beacon data
- Implement data logging for offline analysis and algorithm improvement
- Add replay functionality to test positioning algorithms with recorded data

## Additional Development Information

### Code Style
- Follow Kotlin coding conventions
- Use camelCase for variables and functions
- Use PascalCase for classes and interfaces
- Prefix interface names with 'I' (e.g., ILocationService)
- Group imports by package
- Maximum line length: 100 characters

### Code Structure and Readability
- Keep functions short and focused on a single responsibility
- Limit file size to 200 lines maximum
- Avoid deep nesting (maximum 3 levels of nesting)
- Extract complex conditions into well-named boolean functions
- Replace magic numbers with named constants
- Prefer concise code over verbose implementations
- Use early returns to reduce nesting levels
- Break complex operations into smaller, well-named functions

### Architecture
- The application follows MVVM (Model-View-ViewModel) architecture
- UI is built using Jetpack Compose
- Use ViewModels for UI-related data handling
- Use Repositories for data operations
- Consider using Use Cases for complex business logic
- Implement separate components for:
  - BLE scanning and management
  - Distance estimation from RSSI
  - Position calculation (triangulation)
  - PDR (Pedestrian Dead Reckoning)
  - Sensor fusion
  - Map rendering and coordinate transformation

### Compose UI Development
- Use Material3 components when possible
- Follow the theme defined in `ui/theme/` directory
- Create reusable composable functions for UI elements
- Use previews for UI development
- Test UI components with Compose testing tools

### Performance Considerations
- Minimize object allocations in performance-critical paths
- Use coroutines for asynchronous operations
- Consider using Flows for reactive programming
- Be mindful of battery usage when implementing BLE scanning and sensor monitoring
- Implement proper lifecycle management for resources
- Implement static detection to reduce sensor and BLE scanning frequency when user is not moving
- Use dynamic scanning intervals based on movement speed and positioning requirements
- Consider implementing low-power mode for extended usage scenarios

### Debugging
- Use Android Studio's built-in debugger
- Add logging with appropriate log levels
- Consider using Firebase Crashlytics for production crash reporting