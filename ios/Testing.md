# Testing on iOS Device

## Prerequisites
- Apple Developer account
- Provisioning profile for the app
- USB cable to connect iPhone

## Steps
1. **Connect iPhone** to macOS via USB
2. **Trust** the computer on the iPhone
3. **Open** the project in Xcode
4. **Select** your iPhone as the run destination
5. **Build** and **Run** the app
6. **Allow** permissions (e.g., location, notifications) as prompted

## Notes
- Ensure the backend API URL in `Networking.swift` is accessible from the device.
- If using a local development server, expose it via ngrok or similar.
- For debug logs, connect the iPhone and watch the Xcode console.