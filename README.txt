DEMO CLINIC APP v1.1 - Prabha Creations
APK (install file) GitHub var mofat banva - Android Studio nako
=================================================================

AADHI (ekdach):
  app/src/main/assets/index.html madhe hi oli shodha:
      var DEMO_SRV_URL="",DEMO_SRV_KEY="";
  Tyat Supabase Project URL ani anon key ghala (ek phone var ekdach demo sathi).

1. github.com var login kara (account nasel tar free banva).
2. Varti "+" > New repository. Nav: DemoClinicApp. "Private" nivada. Create repository.
3. "uploading an existing file" var tap kara.
   DemoClinicApp folder madhil SAGLYA files ani folders (app, gradle, gradlew, .github ...)
   tithe odhun taka (drag & drop). Khali "Commit changes".
4. Varti "Actions" tab ughada. "Build APK" chalu hoto (5-8 minite).
   .github folder upload jhala nasel tar: Add file > Create new file,
   nav lihi:  .github/workflows/build.yml
   ani BUILD-WORKFLOW.txt madhil sagla mazkur paste karun Commit kara.
5. Hirvi khun (✓) aali ki tya run var tap kara. Khali "Artifacts" madhe
   "DemoClinic-v1.1-APK" download kara. Tyatil DemoClinic-v1.1.apk mobile var pathva.
6. Mobile var APK ughada > "Install unknown apps" la parvangi dya > Install.

MAHATVACHE:
- app/democlinic.jks hi app chi signing key aahe. Hi file kadhihi badlu naka ani japun theva.
  Key badalli tar phone cha device ID badalto ani demo lock chalat nahi.
- Repository Private theva, karan tyat signing key aahe.
- Navin version sathi fakt index.html badla ani punha upload kara; APK aapoaap banto.

Backup files:  Downloads/DemoClinic      Contact (lock screen): 9869369664
