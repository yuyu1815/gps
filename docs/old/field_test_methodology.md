# Indoor Positioning Field Test Methodology

## Overview

This document outlines the methodology for conducting field tests of the indoor positioning application. Field testing is essential to validate the performance of the positioning algorithms in real-world environments and to identify areas for improvement.

## Test Equipment

### Required Hardware
- Android device with the positioning app installed
- At least 4 BLE beacons with known TxPower values
- Measuring tape or laser distance meter (minimum 20m range)
- Tripod or beacon mounting equipment
- Reference markers for ground truth positions
- Power banks for extended testing sessions

### Required Software
- Indoor Positioning Application (debug build)
- Field Test Data Collection Tool (included in the app)
- Data Analysis Spreadsheet Template

## Test Environment Setup

### Beacon Placement
1. Select an open indoor area of at least 10m x 10m
2. Place beacons at different heights (2-3m recommended) around the perimeter
3. Ensure beacons have clear line-of-sight to the testing area
4. Measure and record the exact position of each beacon
5. Configure the app with the correct beacon positions

### Reference Grid
1. Create a reference grid on the floor using tape or markers
2. Use a 1m or 2m grid spacing for accuracy
3. Number or label each grid intersection for easy reference
4. Measure and record the exact coordinates of each reference point

## Test Procedures

### Calibration
1. Measure RSSI at 1m distance from each beacon
2. Configure TxPower values in the app based on measurements
3. Adjust environmental factor based on the specific test environment
4. Verify distance estimation accuracy at known distances (1m, 3m, 5m, 10m)

### Static Position Tests
1. Place the test device at each reference point
2. Record positioning data for at least 30 seconds at each point
3. Test different device orientations (horizontal, vertical, various angles)
4. Test with different device heights (floor level, 1m, 1.5m)
5. Repeat measurements at different times of day to account for environmental variations

### Dynamic Movement Tests
1. Define standard walking paths using the reference grid
2. Walk along the paths at a consistent pace (approximately 1m/s)
3. Record continuous positioning data during movement
4. Test different walking patterns:
   - Straight lines
   - 90-degree turns
   - Circular paths
   - Random walking
5. Test different walking speeds (slow, normal, fast)

### Interference Tests
1. Introduce common sources of interference:
   - People walking through the test area
   - Wi-Fi access points
   - Bluetooth devices
   - Metal objects
   - Glass surfaces
2. Record positioning data with and without interference
3. Document the impact of each interference source

## Data Collection

### Metrics to Record
1. Estimated position (x, y coordinates)
2. Position confidence value
3. Number of beacons used for positioning
4. RSSI values for each beacon
5. Calculated distances to each beacon
6. Sensor data (accelerometer, gyroscope, magnetometer)
7. Device orientation
8. Timestamp for each measurement

### Test Scenarios
1. Optimal conditions (clear line-of-sight, minimal interference)
2. Suboptimal conditions (partial line-of-sight, moderate interference)
3. Challenging conditions (limited line-of-sight, significant interference)
4. Edge cases (near walls, corners, outside the beacon perimeter)

## Data Analysis

### Performance Metrics
1. Position Accuracy
   - Mean Error: Average distance between estimated and actual positions
   - Root Mean Square Error (RMSE): Square root of the average squared errors
   - 95th Percentile Error: Error value below which 95% of the measurements fall
   - Maximum Error: Largest observed error

2. Precision
   - Standard Deviation of position estimates at static points
   - Jitter: Variation in consecutive position estimates

3. Reliability
   - Percentage of time with position confidence above 0.7
   - Percentage of time with at least 3 beacons available
   - Frequency of position jumps (>1m change in consecutive estimates)

4. Latency
   - Time to first position fix
   - Update rate (positions per second)
   - Processing time for position calculation

### Visualization
1. Error heatmap overlaid on the test area
2. Actual vs. estimated path traces for dynamic tests
3. Error distribution histograms
4. Time series plots of position error

## Reporting

### Test Report Template
1. Test Environment Description
   - Dimensions and characteristics of the test area
   - Beacon placement and configuration
   - Environmental conditions (temperature, humidity, etc.)

2. Test Execution Summary
   - Date and time of testing
   - Test scenarios executed
   - Deviations from test plan

3. Results
   - Summary statistics for each performance metric
   - Comparison to performance targets
   - Visualizations of key findings

4. Analysis
   - Identified strengths and weaknesses
   - Environmental factors affecting performance
   - Comparison with previous test results

5. Recommendations
   - Algorithm improvements
   - Configuration adjustments
   - Further testing needed

## Field Test Tools

### Data Collection Tool
The application includes a built-in Field Test Mode that can be activated in the debug settings. This tool provides:

1. Automated test sequence execution
2. Ground truth position input
3. Real-time performance metrics display
4. Data logging to CSV files
5. Test report generation

### Usage Instructions
1. Enable Developer Mode in the app settings
2. Navigate to Debug > Field Test Tools
3. Configure the test parameters:
   - Test area dimensions
   - Beacon positions
   - Test scenarios to run
4. Follow the on-screen instructions for each test
5. Export the collected data after completing all tests

### Data Analysis Tool
A companion spreadsheet template is provided for analyzing field test data:

1. Import the CSV data exported from the app
2. Run the automated analysis to calculate performance metrics
3. Generate visualizations of the results
4. Compare with previous test results
5. Export the final test report

## Continuous Improvement

### Feedback Loop
1. Analyze field test results to identify performance issues
2. Implement algorithm improvements based on findings
3. Update configuration parameters based on field measurements
4. Conduct regression testing to verify improvements
5. Document lessons learned for future testing

### Test Schedule
1. Conduct baseline field tests for each major release
2. Perform targeted tests after significant algorithm changes
3. Schedule periodic tests in different environments
4. Maintain a database of test results for trend analysis

## Appendix

### Sample Test Plan
A detailed example of a field test plan for a specific environment:

1. Test Area: University Building Lobby (15m x 20m)
2. Beacon Configuration: 6 beacons at 3m height
3. Test Scenarios:
   - Static positioning at 20 reference points
   - Walking along 5 predefined paths
   - Positioning during peak and off-peak hours
4. Performance Targets:
   - Mean Error < 2.0m
   - 95th Percentile Error < 3.5m
   - Update Rate > 1 Hz

### Troubleshooting Guide
Common issues encountered during field testing and their solutions:

1. Inconsistent RSSI readings
   - Check for interference sources
   - Verify beacon battery levels
   - Adjust beacon placement for better line-of-sight

2. Systematic position bias
   - Recalibrate TxPower values
   - Adjust environmental factor
   - Check for large metal objects affecting signal propagation

3. Poor dynamic tracking
   - Verify sensor calibration
   - Adjust step detection parameters
   - Check PDR and BLE fusion weights