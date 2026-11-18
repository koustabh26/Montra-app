# FAQ

**Does Montra need internet or an account?** No. It declares no permissions, including `INTERNET`.

**Where is my data stored?** In the app's private storage on your device (`montra.db`). Android cloud backup is disabled for the app.

**How do I move data to a new phone?** Settings → *Backup & data* → *Back up to file*, copy it to the new phone, install Montra, then restore it.

**Are backups encrypted?** No. Keep backup and CSV files somewhere private.

**I forgot my PIN.** There is no recovery by design. Clearing the app's data removes the PIN **and all data**; restore from a backup afterwards.

**Why 6 digits only?** Shorter PINs are much easier to guess. Easily guessed PINs (e.g. 123456, 111111) are rejected, and repeated wrong attempts trigger a growing lockout (5 free tries, then 30 s, 1 min, 5 min, 15 min, 1 h).

**Why does the app ask for the PIN again when I come back?** It relocks when it leaves the foreground. Choosing a file for backup/restore does not trigger an extra prompt.

**Can I screenshot the app?** Only if *Screen privacy* is off in Settings.

**Which currency is supported?** Indian rupee (₹) only.

**Is there an iOS version or web version?** No.

**The build fails with "SDK location not found".** Create `local.properties` with `sdk.dir=...`.

**Is there a license?** Not yet; see the README.
