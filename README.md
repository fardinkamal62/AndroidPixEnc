# AndroidPixEnc

**AndroidPixEnc** is an Android application that encrypts an image by manipulating its pixels using a password-seeded permutation. The encrypted image can be shared safely; only someone with the correct password can restore the original image.

This project was built for the **Mobile Application and Development Lab (0613-410)** final lab project.

---

## Student Information

- **Name:** Fardin Kamal
- **Course:** Mobile Application and Development Lab (0613-410)
- **Instructor:** Ekramul Haque Amin
- **Semester:** 7th

---

## GitHub Repository

- **Repository:** [https://github.com/fardinkamal62/AndroidPixEnc](https://github.com/fardinkamal62/AndroidPixEnc)
- **Release v1.0 (APK):** [https://github.com/fardinkamal62/AndroidPixEnc/releases/tag/v1.0](https://github.com/fardinkamal62/AndroidPixEnc/releases/tag/v1.0)

> The repository is public and will remain public after submission.

---

## Local Setup

The Firebase project configuration is intentionally not committed. To run the app:

1. Create a Firebase project and register an Android app with the package name
   `com.fardinkamal.androidpixenc`.
2. Enable **Authentication → Sign-in method → Email/Password**.
3. Create a **Realtime Database**.
4. Download `google-services.json` and place it at `app/google-services.json`.
5. Publish the rules from `database.rules.json` in the Realtime Database Rules tab.
6. Build and run.

The project compiles without `app/google-services.json`, but the app shows a
setup message instead of starting until the file is added.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

### Troubleshooting: `CONFIGURATION_NOT_FOUND` when creating an account

`CONFIGURATION_NOT_FOUND` is a Firebase project setting, not an app bug and not
a CAPTCHA. The `RecaptchaCallWrapper` line in Logcat is just an internal wrapper
inside the Firebase Auth SDK; it does not mean a CAPTCHA is blocking sign-up.

Fix it in the Firebase console:

1. Open **Authentication → Get started** (if Authentication has never been set up).
2. Open **Sign-in method → Email/Password**, enable it, and save.
3. Confirm `app/google-services.json` belongs to the same project as the app
   (`android-pixenc`, package `com.fardinkamal.androidpixenc`), then rebuild.
4. If it still fails, open Google Cloud Console → APIs & Services and confirm
   **Identity Toolkit API** is enabled for the project, and that the Android API
   key is not restricted in a way that blocks Identity Toolkit.

---

## Problem and Goal

Sharing an image privately is difficult: messaging apps and cloud storage may expose the original image. PixEnc solves this by producing a visually meaningless encrypted image that can be shared publicly. The original image can only be recovered by entering the correct password inside the app.

The app also lets each logged-in user save, view, edit, and delete their encrypted-image records using Firebase Authentication and Firebase Realtime Database.

---

## Features

### Core Features

1. **User Registration and Login**
   - Firebase Authentication with email and password.
   - Input validation with error messages using `TextInputEditText.setError()`.

2. **Encrypt an Image**
   - Pick an image from the gallery (or camera).
   - Enter a password.
   - The app generates a deterministic pixel permutation from the password and a random salt.
   - The encrypted image is saved as a **PNG** (JPEG is not used because recompression would destroy the pixel mapping).

3. **Decrypt an Image**
   - Open a saved record.
   - Enter the correct password.
   - The app reverses the pixel permutation and displays the original image.

4. **Save Encrypted Records**
   - Each record is stored in Firebase Realtime Database under the logged-in user.
   - Records include title, note, salt, image data, width, height, and timestamp.

5. **View Saved Records**
   - A `RecyclerView` lists all encrypted-image records for the current user.
   - Each item is displayed in a `CardView`.

6. **Edit Record Details**
   - Edit the title and note of a saved record.

7. **Delete Record**
   - Delete a record using an `AlertDialog` confirmation.

8. **Logout**
   - Firebase Authentication sign-out.

### Android Components Used

- `SplashActivity`, `LoginActivity`, `RegisterActivity`, `HomeActivity`, `EncryptActivity`, `DetailActivity`, `EditActivity`
- `ConstraintLayout` and `LinearLayout`
- `TextView`, `ImageView`, `Button`, `CardView`, `FloatingActionButton`
- `TextInputEditText` and `TextInputLayout`
- `RecyclerView` with a custom `Adapter` and `ViewHolder`
- `AlertDialog`
- `Intent` for navigation and passing data with `putExtra()`
- `SharedPreferences` for session/last-email convenience
- Gallery/Camera intent for image selection
- Firebase Authentication
- Firebase Realtime Database
- Model class: `ImageRecord.java`

---

## Required Feature Coverage

| # | Required Feature | How PixEnc Implements It |
|---|---|---|
| 1 | At least 5 screens with navigation | Splash, Login, Register, Home, Encrypt/Add, Detail/View, Edit |
| 2 | Layouts and basic views | `ConstraintLayout`, `LinearLayout`, `TextView`, `ImageView`, `Button`, `CardView` |
| 3 | User input with validation | Email/password/title/password confirmation validation with `setError()` |
| 4 | Model class | `ImageRecord.java` with getters and setters |
| 5 | Scrolling list of records | `RecyclerView` + Adapter + ViewHolder |
| 6 | CRUD with dialog and FAB | FAB opens Add/Encrypt; items can be viewed, edited, deleted with `AlertDialog` |
| 7 | Firebase Authentication | Login, Register, Logout |
| 8 | Firebase Realtime Database | Stores records under `users/{uid}/records/{recordId}` |
| 9 | SharedPreferences | Remembers last email/session convenience |
| 10 | Camera / Gallery Intent | Image selection from gallery or camera |
| 11 | AlertDialog | Delete confirmation and password prompt |
| 12 | Git + GitHub | Public repository, regular commits, README, Release v1.0 with APK |

---

## Encryption Algorithm

PixEnc uses a **password-seeded Fisher–Yates pixel permutation**.

### Why pixels instead of bytes?

Manipulating raw bytes can corrupt image headers and make the file unreadable. Manipulating pixels keeps the image file valid, so the encrypted image can still be opened and shared.

### Algorithm

1. Convert the password and a stored random salt into a deterministic seed:
   `seed = first 8 bytes of SHA-256(password UTF-8 bytes || raw salt bytes)`
2. Create an index array `p` of size `n`, where `n` is the number of pixels.
3. Shuffle `p` using `java.util.Random(seed)` with the Fisher–Yates algorithm.
4. **Encrypt:** `encryptedPixels[i] = originalPixels[p[i]]`
5. **Decrypt:** `originalPixels[p[i]] = encryptedPixels[i]`
6. Save the encrypted image as **PNG only**.

Because the permutation depends on the password and salt, a slightly different password produces a completely different permutation and therefore a completely different image.

> Note: This project demonstrates pixel permutation/obfuscation for educational purposes. It is not a replacement for modern cryptography such as AES for real-world security.

---

## Firebase Realtime Database Structure

```text
users/
  {uid}/
    records/
      {recordId}/
        title: "My secret image"
        note: "optional note"
        createdAt: 1730000000000
        salt: "randomBase64Salt"
        imageBase64: "..."
        width: 512
        height: 512
